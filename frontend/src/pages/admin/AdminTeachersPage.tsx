import { type FormEvent, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

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
  useAdminTeacherProfile,
  useAdminTeacherProfileModeration,
  useAdminTeacherProfiles,
} from '../../hooks/useAdminTeacherProfiles'
import type {
  AdminTeacherProfileListRequest,
  BackendId,
  TeacherProfileRDO,
  TeacherProfileStatus,
} from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const pageSizeOptions = [10, 20, 50, 100]
const rejectionFields = ['rejectionReason'] as const

type StatusFilter = TeacherProfileStatus | 'all'

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Pending approval', value: 'pending_approval' },
  { label: 'Approved', value: 'approved' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Draft', value: 'draft' },
  { label: 'Suspended', value: 'suspended' },
]

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

const isTeacherStatus = (value?: string): value is TeacherProfileStatus =>
  Boolean(value && statusOptions.some((option) => option.value === value && value !== 'all'))

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)

  if (value !== undefined && value !== '') {
    params.set(key, String(value))
  }
}

function useTeacherApprovalFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<AdminTeacherProfileListRequest>(() => {
    const rawStatus = firstParam(searchParams, 'status')
    const status =
      rawStatus === 'all'
        ? undefined
        : isTeacherStatus(rawStatus)
          ? rawStatus
          : 'pending_approval'

    return {
      page: numberParam(searchParams, 'page', 0),
      size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
      status,
    }
  }, [searchParams])

  const searchText = firstParam(searchParams, 'q') ?? ''
  const statusValue =
    firstParam(searchParams, 'status') === 'all' ? 'all' : filters.status ?? 'pending_approval'

  const updateFilters = (
    updates: Omit<Partial<AdminTeacherProfileListRequest>, 'status'> & {
      q?: string
      status?: StatusFilter
    },
  ) => {
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
      ;['page', 'q', 'size', 'status'].forEach((key) => next.delete(key))
      return next
    })
  }

  return { clearFilters, filters, searchText, statusValue, updateFilters }
}

const profileTone = (status: TeacherProfileStatus) => {
  if (status === 'approved') {
    return 'success'
  }

  if (status === 'rejected' || status === 'suspended') {
    return 'danger'
  }

  return status === 'pending_approval' ? 'warning' : 'neutral'
}

function RejectForm({
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
    ? toFormErrorState(error, rejectionFields, 'Unable to reject teacher profile.')
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
        error={localError ?? serverError?.fieldErrors.rejectionReason}
        label="Rejection reason"
        onChange={(event) => setReason(event.target.value)}
        required
        value={reason}
      />
      <Button isLoading={isLoading} loadingLabel="Rejecting" type="submit" variant="danger">
        Reject teacher
      </Button>
    </form>
  )
}

function TeacherProfileDetail({
  onApprove,
  onReject,
  profileId,
}: {
  onApprove: (profileId: BackendId) => void
  onReject: (profileId: BackendId, reason: string) => void
  profileId?: BackendId
}) {
  const detailQuery = useAdminTeacherProfile(profileId)
  const detail = detailQuery.data
  const profile = detail?.profile
  const { approve, reject } = useAdminTeacherProfileModeration()

  if (detailQuery.isLoading) {
    return <p className="status">Loading teacher profile...</p>
  }

  if (detailQuery.isError || !profile) {
    return (
      <Alert title="Teacher profile could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  return (
    <div className="feature-stack">
      {approve.isError && (
        <Alert title="Decision could not be saved" variant="danger">
          {normalizeApiError(approve.error).message}
        </Alert>
      )}
      <DescriptionList
        items={[
          { label: 'Display name', value: profile.displayName },
          { label: 'Headline', value: profile.headline ?? 'Not provided' },
          { label: 'Status', value: humanizeToken(profile.status) },
          { label: 'Rating', value: `${profile.ratingAverage ?? 0} (${profile.ratingCount})` },
          { label: 'Updated', value: formatDate(profile.updatedAt) },
          { label: 'Approved', value: formatDate(profile.approvedAt) },
        ]}
      />
      {profile.rejectionReason && (
        <Alert title="Previous rejection reason" variant="warning">
          {profile.rejectionReason}
        </Alert>
      )}
      <Panel title="Profile text">
        <div className="feature-panel-body">
          <p className="feature-card__summary">{profile.bio || 'No bio provided.'}</p>
        </div>
      </Panel>
      <Panel title="Content and earnings">
        <div className="feature-panel-body">
          <DescriptionList
            items={[
              { label: 'Total tests', value: detail.totalTestCount },
              { label: 'Published tests', value: detail.publishedTestCount },
              {
                label: 'Wallet available',
                value: detail.wallet
                  ? formatCurrency(detail.wallet.availableAmount, detail.wallet.currency, '0 UZS')
                  : 'No wallet',
              },
            ]}
          />
          {detail.recentTests.length > 0 && (
            <div className="feature-list">
              {detail.recentTests.map((test) => (
                <div className="profile-table-stack" key={test.id}>
                  <strong>{test.title}</strong>
                  <span>
                    {humanizeToken(test.visibility)} - {formatDate(test.updatedAt)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </Panel>
      {profile.status === 'pending_approval' && (
        <div className="teacher-approval-actions">
          <Button
            isLoading={approve.isPending}
            onClick={() => {
              approve.mutate(profile.id, {
                onSuccess: () => onApprove(profile.id),
              })
            }}
            variant="primary"
          >
            Approve teacher
          </Button>
          <RejectForm
            error={reject.error}
            isLoading={reject.isPending}
            onSubmit={(reason) => {
              reject.mutate(
                { payload: { rejectionReason: reason }, profileId: profile.id },
                {
                  onSuccess: () => onReject(profile.id, reason),
                },
              )
            }}
          />
        </div>
      )}
    </div>
  )
}

export function AdminTeachersPage() {
  const { clearFilters, filters, searchText, statusValue, updateFilters } =
    useTeacherApprovalFilters()
  const profilesQuery = useAdminTeacherProfiles(filters)
  const [selectedProfileId, setSelectedProfileId] = useState<BackendId | undefined>()
  const { approve } = useAdminTeacherProfileModeration()
  const profiles = useMemo(() => {
    const normalized = searchText.trim().toLowerCase()
    const items = profilesQuery.data?.items ?? []

    if (!normalized) {
      return items
    }

    return items.filter((profile) =>
      [profile.displayName, profile.headline, profile.bio, String(profile.userId)]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalized)),
    )
  }, [profilesQuery.data?.items, searchText])
  const page = profilesQuery.data?.meta.page
  const [decisionMessage, setDecisionMessage] = useState<string | null>(null)

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Teachers" />

      {decisionMessage && (
        <Alert title="Decision saved" variant="success">
          {decisionMessage}
        </Alert>
      )}

      {approve.isError && (
        <Alert title="Approval failed" variant="danger">
          {normalizeApiError(approve.error).message}
        </Alert>
      )}

      <Panel title="Teacher approval queue" description="Review profile submissions and status history.">
        <FilterBar onClear={clearFilters}>
          <TextField
            helpText="Filters the loaded page only; the backend contract exposes status paging."
            label="Search current page"
            onChange={(event) => updateFilters({ page: 0, q: event.target.value })}
            placeholder="Name, headline, bio, user id"
            value={searchText}
          />
          <SelectField
            label="Status"
            onChange={(event) =>
              updateFilters({
                page: 0,
                status: event.target.value as StatusFilter,
              })
            }
            value={statusValue}
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Page size"
            onChange={(event) =>
              updateFilters({ page: 0, size: Number(event.target.value) || DEFAULT_PAGE_SIZE })
            }
            value={filters.size ?? DEFAULT_PAGE_SIZE}
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </SelectField>
        </FilterBar>

        <DataTable<TeacherProfileRDO>
          columns={[
            {
              header: 'Teacher',
              key: 'teacher',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>{row.displayName}</strong>
                  <span>{row.headline ?? 'No headline'}</span>
                </div>
              ),
            },
            {
              header: 'Status',
              key: 'status',
              render: (row) => (
                <Badge tone={profileTone(row.status)}>{humanizeToken(row.status)}</Badge>
              ),
            },
            { header: 'Updated', key: 'updatedAt', render: (row) => formatDate(row.updatedAt) },
            {
              header: 'Rating',
              key: 'rating',
              render: (row) => `${row.ratingAverage ?? 0} (${row.ratingCount})`,
            },
            {
              header: 'Actions',
              key: 'actions',
              render: (row) => (
                <div className="feature-card__actions">
                  <Button onClick={() => setSelectedProfileId(row.id)} size="sm" variant="secondary">
                    Review
                  </Button>
                  {row.status === 'pending_approval' && (
                    <Button
                      isLoading={approve.isPending}
                      onClick={() =>
                        approve.mutate(row.id, {
                          onSuccess: () => setDecisionMessage(`${row.displayName} approved.`),
                        })
                      }
                      size="sm"
                    >
                      Approve
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
          emptyMessage="No teacher profiles match the selected filters."
          emptyTitle="No teachers"
          error={
            profilesQuery.isError ? normalizeApiError(profilesQuery.error).message : undefined
          }
          getRowKey={(row) => row.id}
          isLoading={profilesQuery.isLoading}
          rows={profiles}
        />

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={profilesQuery.isFetching}
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
        description="Review public profile details, content references, and wallet state before deciding."
        isOpen={Boolean(selectedProfileId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedProfileId(undefined)
          }
        }}
        title="Teacher review"
      >
        <TeacherProfileDetail
          onApprove={() => setDecisionMessage('Teacher profile approved.')}
          onReject={(_, reason) => setDecisionMessage(`Teacher profile rejected: ${reason}`)}
          profileId={selectedProfileId}
        />
      </Dialog>
    </PageContainer>
  )
}
