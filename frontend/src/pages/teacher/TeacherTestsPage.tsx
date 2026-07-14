import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  DataTable,
  Dialog,
  FilterBar,
  PageContainer,
  PageHeader,
  Pagination,
  SelectField,
  TextField,
} from '../../components/ui'
import { ROUTES, fillRoute } from '../../constants/routes'
import {
  canEditTeacherTest,
  useTeacherTestBuilderMutations,
  useTeacherTests,
} from '../../hooks/useTeacherTests'
import type {
  SortQueryValue,
  TeacherTestListRequest,
  TeacherTestSummaryRDO,
  TestVisibilityStatus,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import {
  formatCurrency,
  formatDate,
  formatMinutes,
  formatNumber,
  humanizeToken,
} from '../../utils/formatters'

type StatusFilterValue = TestVisibilityStatus | 'all'

const DEFAULT_PAGE_SIZE = 20
const DEFAULT_SORT: SortQueryValue = 'updatedAt,desc'

const statusOptions: Array<{ label: string; value: StatusFilterValue }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Draft', value: 'draft' },
  { label: 'Submitted', value: 'submitted' },
  { label: 'Published', value: 'published' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Archived', value: 'archived' },
]

const sortOptions: Array<{ label: string; value: SortQueryValue }> = [
  { label: 'Recently updated', value: 'updatedAt,desc' },
  { label: 'Oldest updated', value: 'updatedAt,asc' },
  { label: 'Newest created', value: 'createdAt,desc' },
  { label: 'Title A-Z', value: 'title,asc' },
  { label: 'Status', value: 'visibility,asc' },
  { label: 'IELTS type', value: 'type,asc' },
]

const pageSizeOptions = [10, 20, 50, 100]

const editableStatuses = new Set(['draft', 'rejected'])
const submittableStatuses = new Set(['draft', 'rejected'])
const archivableStatuses = new Set(['draft', 'rejected', 'published'])

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value && value.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string, fallback: number) => {
  const value = firstParam(params, key)
  if (!value) {
    return fallback
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const pageSizeParam = (params: URLSearchParams) => {
  const size = numberParam(params, 'size', DEFAULT_PAGE_SIZE)
  return pageSizeOptions.includes(size) ? size : DEFAULT_PAGE_SIZE
}

const isStatusFilter = (value?: string): value is TestVisibilityStatus =>
  Boolean(value && statusOptions.some((option) => option.value === value && value !== 'all'))

const setParam = (
  params: URLSearchParams,
  key: string,
  value: number | string | undefined,
) => {
  params.delete(key)

  if (value === undefined || value === '') {
    return
  }

  params.set(key, String(value))
}

function useTeacherListFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<TeacherTestListRequest>(() => {
    const status = firstParam(searchParams, 'status')

    return {
      page: numberParam(searchParams, 'page', 0),
      q: firstParam(searchParams, 'q'),
      size: pageSizeParam(searchParams),
      sort: (firstParam(searchParams, 'sort') as SortQueryValue | undefined) ?? DEFAULT_SORT,
      status: isStatusFilter(status) ? status : undefined,
    }
  }, [searchParams])

  const updateFilters = (updates: Partial<TeacherTestListRequest>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)

      Object.entries(updates).forEach(([key, value]) => {
        setParam(next, key, value as number | string | undefined)
      })

      return next
    })
  }

  const clearFilters = () => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('q')
      next.delete('status')
      next.delete('page')
      next.delete('sort')
      next.delete('size')
      return next
    })
  }

  return { clearFilters, filters, updateFilters }
}

const statusTone = (status: string) => {
  if (status === 'published') {
    return 'success'
  }

  if (status === 'rejected') {
    return 'danger'
  }

  if (status === 'submitted') {
    return 'warning'
  }

  return status === 'archived' ? 'neutral' : 'info'
}

const errorMessage = (error: unknown, fallback: string) =>
  normalizeApiError(error).message || fallback

const testEditPath = (test: TeacherTestSummaryRDO) =>
  fillRoute(ROUTES.teacherTestEdit, { id: test.id })

const canSubmit = (test: TeacherTestSummaryRDO) =>
  submittableStatuses.has(String(test.visibility))

const canArchive = (test: TeacherTestSummaryRDO) =>
  archivableStatuses.has(String(test.visibility))

const actionErrorTitle = (error: unknown) => {
  const normalized = normalizeApiError(error)

  if (normalized.code === 'VALIDATION_FAILED') {
    return 'Readiness validation failed'
  }

  if (normalized.code === 'TEACHER_TEST_STATUS_CONFLICT') {
    return 'Status changed'
  }

  return humanizeToken(normalized.code)
}

function ActionError({ error }: { error: unknown }) {
  const normalized = normalizeApiError(error)

  return (
    <Alert title={actionErrorTitle(error)} variant="danger">
      <p>{normalized.message}</p>
      {normalized.fieldErrors.length > 0 && (
        <ul className="teacher-tests-error-list">
          {normalized.fieldErrors.map((fieldError) => (
            <li key={`${fieldError.field}-${fieldError.code}`}>
              <strong>{fieldError.field}:</strong> {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </Alert>
  )
}

function TestActions({ test }: { test: TeacherTestSummaryRDO }) {
  const [archiveDialogOpen, setArchiveDialogOpen] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const mutations = useTeacherTestBuilderMutations(test.id)
  const isMutating = mutations.submitTest.isPending || mutations.archiveTest.isPending
  const editable = canEditTeacherTest(test)
  const editLabel = editableStatuses.has(String(test.visibility)) ? 'Edit' : 'Open preview'

  const submitTest = () => {
    setActionError(null)
    mutations.submitTest.mutate(undefined, {
      onError: setActionError,
    })
  }

  const archiveTest = () => {
    setActionError(null)
    mutations.archiveTest.mutate(undefined, {
      onError: setActionError,
      onSuccess: () => setArchiveDialogOpen(false),
    })
  }

  return (
    <div className="teacher-tests-actions">
      {actionError !== null && !archiveDialogOpen && <ActionError error={actionError} />}
      <div className="teacher-tests-actions__buttons">
        <Link className="feature-link" to={testEditPath(test)}>
          {editLabel}
        </Link>
        {!editable && (
          <span className="teacher-tests-action-note">
            {humanizeToken(String(test.visibility))} tests are read-only.
          </span>
        )}
        {canSubmit(test) && (
          <Button
            disabled={isMutating}
            isLoading={mutations.submitTest.isPending}
            loadingLabel="Submitting"
            onClick={submitTest}
            size="sm"
            variant="secondary"
          >
            Submit
          </Button>
        )}
        {canArchive(test) && (
          <Button
            disabled={isMutating}
            onClick={() => setArchiveDialogOpen(true)}
            size="sm"
            variant="danger"
          >
            Archive
          </Button>
        )}
      </div>

      <Dialog
        description="Archived tests are removed from active management views but stay recoverable in your archive."
        footer={
          <>
            <Button
              disabled={mutations.archiveTest.isPending}
              onClick={() => setArchiveDialogOpen(false)}
              variant="secondary"
            >
              Cancel
            </Button>
            <Button
              isLoading={mutations.archiveTest.isPending}
              loadingLabel="Archiving"
              onClick={archiveTest}
              variant="danger"
            >
              Archive test
            </Button>
          </>
        }
        isOpen={archiveDialogOpen}
        onOpenChange={setArchiveDialogOpen}
        title={`Archive ${test.title}`}
      >
        <p>
          This will move the test to archived status. Submitted tests cannot be archived until
          moderation completes.
        </p>
        {actionError !== null && <ActionError error={actionError} />}
      </Dialog>
    </div>
  )
}

function TestTitleCell({ test }: { test: TeacherTestSummaryRDO }) {
  const rejected = test.visibility === 'rejected'

  return (
    <div className="teacher-tests-title-cell">
      <Link to={testEditPath(test)}>{test.title}</Link>
      <p>{test.description || 'No description provided.'}</p>
      <div className="teacher-tests-meta">
        <span>{humanizeToken(String(test.type))}</span>
        <span>{formatMinutes(test.durationMinutes)}</span>
        <span>{test.free ? 'Free' : formatCurrency(test.priceAmount, test.currency ?? 'UZS')}</span>
      </div>
      {rejected && (
        <Alert title="Rejected" variant="danger">
          {test.moderationNote ||
            'The moderation team requested changes before this test can be published.'}{' '}
          <Link to={testEditPath(test)}>Return to editing</Link>
        </Alert>
      )}
    </div>
  )
}

function emptyStateCopy(filters: TeacherTestListRequest) {
  if (filters.q) {
    return {
      message: 'Try a different search term or clear filters to see all owned tests.',
      title: 'No tests match this search',
    }
  }

  if (filters.status) {
    return {
      message: `No ${humanizeToken(filters.status).toLowerCase()} tests are in this view right now.`,
      title: `No ${humanizeToken(filters.status).toLowerCase()} tests`,
    }
  }

  return {
    message: 'Create a draft to start assembling IELTS sections and questions.',
    title: 'No teacher tests yet',
  }
}

export function TeacherTestsPage() {
  const { clearFilters, filters, updateFilters } = useTeacherListFilters()
  const testsQuery = useTeacherTests(filters)
  const pageMeta = testsQuery.data?.meta.page
  const tests = testsQuery.data?.items ?? []
  const emptyCopy = emptyStateCopy(filters)
  const hasActiveFilters = Boolean(filters.q || filters.status)

  return (
    <PageContainer size="wide">
      <PageHeader
        actions={
          <Link className="feature-link feature-link--primary" to={ROUTES.teacherTestCreate}>
            Create new test
          </Link>
        }
        eyebrow="Teacher"
        title="Tests"
      />

      <section className="ui-admin-table" aria-label="Teacher tests">
        <div className="ui-admin-table__header">
          <div>
            <h2>Owned tests</h2>
            <p>Manage drafts, moderation submissions, published tests, rejected tests, and archives.</p>
          </div>
        </div>

        <div className="ui-admin-table__filters">
          <FilterBar onClear={hasActiveFilters ? clearFilters : undefined}>
            <TextField
              label="Search"
              onChange={(event) => updateFilters({ page: 0, q: event.target.value || undefined })}
              placeholder="Title or description"
              type="search"
              value={filters.q ?? ''}
            />
            <SelectField
              label="Status"
              onChange={(event) =>
                updateFilters({
                  page: 0,
                  status:
                    event.target.value === 'all'
                      ? undefined
                      : (event.target.value as TestVisibilityStatus),
                })
              }
              value={filters.status ?? 'all'}
            >
              {statusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </SelectField>
            <SelectField
              label="Sort"
              onChange={(event) =>
                updateFilters({ page: 0, sort: event.target.value as SortQueryValue })
              }
              value={String(filters.sort ?? DEFAULT_SORT)}
            >
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </SelectField>
            <SelectField
              label="Page size"
              onChange={(event) => updateFilters({ page: 0, size: Number(event.target.value) })}
              value={String(filters.size ?? DEFAULT_PAGE_SIZE)}
            >
              {pageSizeOptions.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </SelectField>
          </FilterBar>
        </div>

        <DataTable
          columns={[
            {
              header: 'Test',
              key: 'test',
              render: (test) => <TestTitleCell test={test} />,
            },
            {
              header: 'Status',
              key: 'status',
              render: (test) => (
                <div className="teacher-tests-table-stack">
                  <Badge tone={statusTone(String(test.visibility))}>
                    {humanizeToken(String(test.visibility))}
                  </Badge>
                  <span>Updated {formatDate(test.updatedAt)}</span>
                  {test.publishedAt && <span>Published {formatDate(test.publishedAt)}</span>}
                </div>
              ),
            },
            {
              align: 'right',
              header: 'Band',
              key: 'band',
              render: (test) => (
                <span>
                  {formatNumber(test.estimatedBandMin, '-')} -{' '}
                  {formatNumber(test.estimatedBandMax, '-')}
                </span>
              ),
            },
            {
              align: 'right',
              header: 'Actions',
              key: 'actions',
              render: (test) => <TestActions test={test} />,
            },
          ]}
          emptyMessage={emptyCopy.message}
          emptyTitle={emptyCopy.title}
          error={
            testsQuery.isError
              ? errorMessage(testsQuery.error, 'Owned tests could not be loaded.')
              : undefined
          }
          getRowKey={(test) => test.id}
          isLoading={testsQuery.isLoading}
          rows={tests}
        />

        {!testsQuery.isLoading && !testsQuery.isError && tests.length === 0 && (
          <div className="teacher-tests-empty-actions">
            {hasActiveFilters ? (
              <Button onClick={clearFilters} variant="secondary">
                Clear filters
              </Button>
            ) : (
              <Link className="feature-link feature-link--primary" to={ROUTES.teacherTestCreate}>
                Create first test
              </Link>
            )}
          </div>
        )}

        {pageMeta && tests.length > 0 && (
          <Pagination
            canGoNext={!pageMeta.last}
            canGoPrevious={!pageMeta.first}
            isLoading={testsQuery.isFetching}
            onNext={() => updateFilters({ page: pageMeta.number + 1 })}
            onPrevious={() => updateFilters({ page: Math.max(0, pageMeta.number - 1) })}
            page={pageMeta.number + 1}
            pageSize={pageMeta.size}
            totalItems={pageMeta.totalElements}
            totalPages={pageMeta.totalPages}
          />
        )}
      </section>
    </PageContainer>
  )
}
