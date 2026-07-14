import { type FormEvent, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { BuilderPreviewPanel, type BuilderPreviewMode } from '../../components/test-builder/BuilderPreviewPanel'
import type { BuilderReadinessReport } from '../../components/test-builder/builderReadiness'
import {
  Alert,
  Badge,
  Button,
  DataTable,
  DescriptionList,
  Dialog,
  FilterBar,
  PageContainer,
  PageHeader,
  Pagination,
  Panel,
  SelectField,
  TextareaField,
  TextField,
} from '../../components/ui'
import {
  useAdminModerationDecisions,
  useAdminModerationMeta,
  useAdminModerationTest,
  useAdminModerationTests,
} from '../../hooks/useAdminModeration'
import type {
  AdminModerationTestListRequest,
  AdminModerationTestRDO,
  ApiFieldError,
  BackendId,
  TestType,
} from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatCurrency, formatDate, formatNumber, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const pageSizeOptions = [10, 20, 50, 100]
const rejectFields = ['reason'] as const

const typeOptions: Array<{ label: string; value: TestType | 'all' }> = [
  { label: 'All types', value: 'all' },
  { label: 'Reading', value: 'reading' },
  { label: 'Listening', value: 'listening' },
  { label: 'Writing task 1', value: 'writing_task_1' },
  { label: 'Writing task 2', value: 'writing_task_2' },
  { label: 'Speaking part 1', value: 'speaking_part_1' },
  { label: 'Speaking part 2', value: 'speaking_part_2' },
  { label: 'Speaking part 3', value: 'speaking_part_3' },
  { label: 'Full mock', value: 'full_mock' },
]

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value?.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string, fallback: number) => {
  const parsed = Number(firstParam(params, key))
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)
  if (value !== undefined && value !== '' && value !== 'all') {
    params.set(key, String(value))
  }
}

const dayStart = (value?: string) => (value ? `${value}T00:00:00.000Z` : undefined)
const dayEnd = (value?: string) => (value ? `${value}T23:59:59.999Z` : undefined)
const dateInputValue = (value?: string) => (value && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : '')

function useModerationFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<AdminModerationTestListRequest>(() => ({
    difficulty: firstParam(searchParams, 'difficulty'),
    page: numberParam(searchParams, 'page', 0),
    search: firstParam(searchParams, 'search'),
    size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
    submittedFrom: dayStart(firstParam(searchParams, 'submittedFrom')),
    submittedTo: dayEnd(firstParam(searchParams, 'submittedTo')),
    teacherProfileId: firstParam(searchParams, 'teacherProfileId')
      ? Number(firstParam(searchParams, 'teacherProfileId'))
      : undefined,
    type: firstParam(searchParams, 'type'),
  }), [searchParams])

  const updateFilters = (updates: Record<string, number | string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(updates).forEach(([key, value]) => setParam(next, key, value))
      return next
    })
  }

  const clearFilters = () => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      ;['difficulty', 'page', 'search', 'size', 'submittedFrom', 'submittedTo', 'teacherProfileId', 'type'].forEach((key) =>
        next.delete(key),
      )
      return next
    })
  }

  return {
    clearFilters,
    filters,
    submittedFrom: dateInputValue(firstParam(searchParams, 'submittedFrom')),
    submittedTo: dateInputValue(firstParam(searchParams, 'submittedTo')),
    updateFilters,
  }
}

const validationFieldErrors = (findings?: Record<string, string>): ApiFieldError[] =>
  Object.entries(findings ?? {}).map(([field, message]) => ({
    code: 'VALIDATION_FAILED',
    field,
    message,
  }))

const moderationReadinessReport = (
  readyForDecision: boolean,
  findings: ApiFieldError[],
): BuilderReadinessReport => ({
  blockedCount: readyForDecision ? 0 : Math.max(findings.length, 1),
  readyToSubmit: readyForDecision,
  warningCount: 0,
  items: [
    {
      detail: readyForDecision
        ? 'The backend readiness check allows an admin decision.'
        : 'The backend readiness check blocks approval until the teacher fixes the listed issues.',
      id: 'server-readiness',
      label: 'Backend moderation readiness',
      status: readyForDecision ? 'ready' : 'blocked',
    },
    ...findings.map((finding) => ({
      detail: finding.message,
      id: finding.field,
      label: finding.field,
      status: 'blocked' as const,
    })),
  ],
})

function RejectModerationForm({
  error,
  isLoading,
  onSubmit,
}: {
  error?: unknown
  isLoading: boolean
  onSubmit: (reason: string) => void
}) {
  const [reason, setReason] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)
  const serverError = error
    ? toFormErrorState(error, rejectFields, 'Unable to reject submitted test.')
    : null

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = reason.trim()
    if (!trimmed) {
      setLocalError('Rejection reason is required.')
      return
    }

    setLocalError(null)
    onSubmit(trimmed)
  }

  return (
    <form className="feature-stack" onSubmit={submit}>
      {serverError?.formMessage && (
        <Alert title="Reject failed" variant="danger">
          {serverError.formMessage}
        </Alert>
      )}
      <TextareaField
        disabled={isLoading}
        error={localError ?? serverError?.fieldErrors.reason}
        label="Rejection reason"
        onChange={(event) => setReason(event.target.value)}
        required
        value={reason}
      />
      <Button isLoading={isLoading} loadingLabel="Rejecting" type="submit" variant="danger">
        Reject test
      </Button>
    </form>
  )
}

function ModerationDetail({
  onDecision,
  testId,
}: {
  onDecision: (message: string) => void
  testId?: BackendId
}) {
  const [mode, setMode] = useState<BuilderPreviewMode>('author')
  const detailQuery = useAdminModerationTest(testId)
  const decisions = useAdminModerationDecisions()
  const detail = detailQuery.data
  const test = detail?.test
  const fieldErrors = validationFieldErrors(detail?.validationFindings)

  if (detailQuery.isLoading) {
    return <p className="status">Loading submitted test...</p>
  }

  if (detailQuery.isError || !detail || !test) {
    return (
      <Alert title="Submitted test could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  const metadata = {
    currency: test.currency ?? 'UZS',
    description: test.description ?? '',
    difficulty: test.difficulty ?? '',
    durationMinutes: String(test.durationMinutes ?? ''),
    estimatedBandMax: String(test.estimatedBandMax ?? ''),
    estimatedBandMin: String(test.estimatedBandMin ?? ''),
    free: Boolean(test.free),
    language: test.language ?? '',
    priceAmount: String(test.priceAmount ?? ''),
    title: test.title,
    type: test.type,
  }

  return (
    <div className="feature-stack">
      {decisions.approve.isError && (
        <Alert title="Approval failed" variant="danger">
          {normalizeApiError(decisions.approve.error).message}
        </Alert>
      )}
      <DescriptionList
        items={[
          { label: 'Teacher profile', value: test.teacherProfileId },
          { label: 'Visibility', value: humanizeToken(String(test.visibility)) },
          { label: 'Updated', value: formatDate(test.updatedAt) },
          { label: 'Ready for decision', value: detail.readyForDecision ? 'Yes' : 'No' },
        ]}
      />
      {!detail.readyForDecision && (
        <Alert title="Approval blocked" variant="warning">
          Backend readiness checks found issues. Rejection remains available so the teacher can fix
          the submitted test.
        </Alert>
      )}
      <BuilderPreviewPanel
        activeSectionId={null}
        fieldErrors={fieldErrors}
        hasMetadataDraft={false}
        metadata={metadata}
        mode={mode}
        onModeChange={setMode}
        readinessReport={moderationReadinessReport(detail.readyForDecision, fieldErrors)}
        sectionDirty={false}
        sectionDraft={{ instructions: '', passageText: '' }}
        test={test}
      />
      <div className="teacher-approval-actions">
        <Button
          disabled={!detail.readyForDecision}
          isLoading={decisions.approve.isPending}
          onClick={() =>
            decisions.approve.mutate(test.id, {
              onSuccess: () => onDecision(`${test.title} approved and published.`),
            })
          }
        >
          Approve test
        </Button>
        <RejectModerationForm
          error={decisions.reject.error}
          isLoading={decisions.reject.isPending}
          onSubmit={(reason) =>
            decisions.reject.mutate(
              { payload: { reason }, testId: test.id },
              { onSuccess: () => onDecision(`${test.title} rejected.`) },
            )
          }
        />
      </div>
    </div>
  )
}

const moderationTone = (row: AdminModerationTestRDO) =>
  row.submittedAt ? 'warning' : 'neutral'

export function AdminModerationPage() {
  const { clearFilters, filters, submittedFrom, submittedTo, updateFilters } = useModerationFilters()
  const testsQuery = useAdminModerationTests(filters)
  const metaQuery = useAdminModerationMeta()
  const [selectedTestId, setSelectedTestId] = useState<BackendId | undefined>()
  const [decisionMessage, setDecisionMessage] = useState<string | null>(null)
  const page = testsQuery.data?.meta.page

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Test moderation" />

      {decisionMessage && (
        <Alert title="Decision saved" variant="success">
          {decisionMessage}
        </Alert>
      )}

      <Panel title="Submitted queue" description="Oldest submitted tests are reviewed first.">
        {metaQuery.data && (
          <div className="feature-panel-body">
            <DescriptionList
              items={[
                { label: 'Pending', value: formatNumber(metaQuery.data.pendingCount, '0') },
                { label: 'Oldest submission', value: formatDate(metaQuery.data.oldestSubmittedAt) },
              ]}
            />
          </div>
        )}

        <FilterBar onClear={clearFilters}>
          <TextField
            label="Search"
            onChange={(event) => updateFilters({ page: 0, search: event.target.value })}
            placeholder="Title"
            value={filters.search ?? ''}
          />
          <SelectField
            label="Type"
            onChange={(event) => updateFilters({ page: 0, type: event.target.value })}
            value={filters.type ?? 'all'}
          >
            {typeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
          <TextField
            label="Teacher profile id"
            min={0}
            onChange={(event) => updateFilters({ page: 0, teacherProfileId: event.target.value })}
            type="number"
            value={filters.teacherProfileId ?? ''}
          />
          <TextField
            label="Difficulty"
            onChange={(event) => updateFilters({ page: 0, difficulty: event.target.value })}
            value={filters.difficulty ?? ''}
          />
          <TextField
            label="Submitted from"
            onChange={(event) => updateFilters({ page: 0, submittedFrom: event.target.value })}
            type="date"
            value={submittedFrom}
          />
          <TextField
            label="Submitted to"
            onChange={(event) => updateFilters({ page: 0, submittedTo: event.target.value })}
            type="date"
            value={submittedTo}
          />
          <SelectField
            label="Page size"
            onChange={(event) => updateFilters({ page: 0, size: Number(event.target.value) })}
            value={filters.size ?? DEFAULT_PAGE_SIZE}
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </SelectField>
        </FilterBar>

        <DataTable<AdminModerationTestRDO>
          columns={[
            {
              header: 'Test',
              key: 'test',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>{row.title}</strong>
                  <span>{row.teacherDisplayName}</span>
                </div>
              ),
            },
            {
              header: 'Type',
              key: 'type',
              render: (row) => <Badge tone="info">{humanizeToken(String(row.type))}</Badge>,
            },
            { header: 'Difficulty', key: 'difficulty', render: (row) => humanizeToken(row.difficulty) },
            {
              header: 'Price',
              key: 'price',
              render: (row) => (row.free ? 'Free' : formatCurrency(row.priceAmount, row.currency)),
            },
            {
              header: 'Submitted',
              key: 'submittedAt',
              render: (row) => <Badge tone={moderationTone(row)}>{formatDate(row.submittedAt)}</Badge>,
            },
            {
              header: 'Actions',
              key: 'actions',
              render: (row) => (
                <Button onClick={() => setSelectedTestId(row.id)} size="sm" variant="secondary">
                  Review
                </Button>
              ),
            },
          ]}
          emptyMessage="No submitted tests match the selected filters."
          emptyTitle="No submitted tests"
          error={testsQuery.isError ? normalizeApiError(testsQuery.error).message : undefined}
          getRowKey={(row) => row.id}
          isLoading={testsQuery.isLoading}
          rows={testsQuery.data?.items ?? []}
        />

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={testsQuery.isFetching}
            onNext={() => updateFilters({ page: (filters.page ?? 0) + 1 })}
            onPrevious={() => updateFilters({ page: Math.max((filters.page ?? 0) - 1, 0) })}
            page={page.number + 1}
            pageSize={page.size}
            totalItems={page.totalElements}
            totalPages={page.totalPages}
          />
        )}
      </Panel>

      <Dialog
        description="Read-only student preview plus admin answer keys and backend validation findings."
        isOpen={Boolean(selectedTestId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedTestId(undefined)
          }
        }}
        title="Submitted test review"
      >
        <ModerationDetail
          onDecision={(message) => {
            setDecisionMessage(message)
            setSelectedTestId(undefined)
          }}
          testId={selectedTestId}
        />
      </Dialog>
    </PageContainer>
  )
}
