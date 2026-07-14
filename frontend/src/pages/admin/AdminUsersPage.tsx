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
  useAdminUser,
  useAdminUsers,
  useAdminUserStatusMutation,
} from '../../hooks/useAdminUsers'
import type {
  AdminUserListRequest,
  AdminUserRDO,
  AdminUserRoleFilter,
  AdminUserStatus,
  BackendId,
  TeacherProfileStatus,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const pageSizeOptions = [10, 20, 50, 100]

type AnyFilter = 'all'
type StatusFilter = AdminUserStatus | AnyFilter
type RoleFilter = AdminUserRoleFilter | AnyFilter
type TeacherStatusFilter = TeacherProfileStatus | AnyFilter

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Active', value: 'active' },
  { label: 'Suspended', value: 'suspended' },
  { label: 'Banned', value: 'banned' },
  { label: 'Deleted', value: 'deleted' },
]

const roleOptions: Array<{ label: string; value: RoleFilter }> = [
  { label: 'All roles', value: 'all' },
  { label: 'Students', value: 'student' },
  { label: 'Teachers', value: 'teacher' },
  { label: 'Admins', value: 'admin' },
  { label: 'Moderators', value: 'moderator' },
  { label: 'School managers', value: 'school_manager' },
]

const teacherStatusOptions: Array<{ label: string; value: TeacherStatusFilter }> = [
  { label: 'Any teacher status', value: 'all' },
  { label: 'Draft', value: 'draft' },
  { label: 'Pending approval', value: 'pending_approval' },
  { label: 'Approved', value: 'approved' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Suspended', value: 'suspended' },
]

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value?.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string, fallback: number) => {
  const parsed = Number(firstParam(params, key))
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const isOptionValue = <TValue extends string>(
  value: string | undefined,
  options: Array<{ value: TValue }>,
): value is Exclude<TValue, 'all'> =>
  Boolean(value && value !== 'all' && options.some((option) => option.value === value))

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)

  if (value !== undefined && value !== '' && value !== 'all') {
    params.set(key, String(value))
  }
}

const statusTone = (status: string) => {
  if (status === 'active') {
    return 'success'
  }

  if (status === 'suspended') {
    return 'warning'
  }

  if (status === 'banned' || status === 'deleted') {
    return 'danger'
  }

  return 'neutral'
}

function useAdminUserFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<AdminUserListRequest>(() => {
    const rawStatus = firstParam(searchParams, 'status')
    const rawRole = firstParam(searchParams, 'role')
    const rawTeacherStatus = firstParam(searchParams, 'teacherStatus')

    return {
      page: numberParam(searchParams, 'page', 0),
      q: firstParam(searchParams, 'q'),
      role: isOptionValue(rawRole, roleOptions) ? rawRole : undefined,
      size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
      status: isOptionValue(rawStatus, statusOptions) ? rawStatus : undefined,
      teacherStatus: isOptionValue(rawTeacherStatus, teacherStatusOptions)
        ? rawTeacherStatus
        : undefined,
    }
  }, [searchParams])

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
      ;['page', 'q', 'role', 'size', 'status', 'teacherStatus'].forEach((key) =>
        next.delete(key),
      )
      return next
    })
  }

  return {
    clearFilters,
    filters,
    roleValue: filters.role ?? 'all',
    statusValue: filters.status ?? 'all',
    teacherStatusValue: filters.teacherStatus ?? 'all',
    updateFilters,
  }
}

function UserStatusForm({
  onSaved,
  user,
}: {
  onSaved: (user: AdminUserRDO) => void
  user: AdminUserRDO
}) {
  const updateStatus = useAdminUserStatusMutation()
  const [status, setStatus] = useState<AdminUserStatus>(
    statusOptions.some((option) => option.value === user.status)
      ? (user.status as AdminUserStatus)
      : 'active',
  )
  const [reason, setReason] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (status !== 'active' && reason.trim().length === 0) {
      setLocalError('Add a reason so the account notification is useful.')
      return
    }

    setLocalError(null)
    updateStatus.mutate(
      {
        payload: {
          reason: reason.trim() || undefined,
          status,
        },
        userId: user.id,
      },
      { onSuccess: onSaved },
    )
  }

  return (
    <form className="feature-stack" onSubmit={submit}>
      {updateStatus.isError && (
        <Alert title="Status could not be updated" variant="danger">
          {normalizeApiError(updateStatus.error).message}
        </Alert>
      )}
      <SelectField
        label="Account status"
        onChange={(event) => setStatus(event.target.value as AdminUserStatus)}
        value={status}
      >
        {statusOptions
          .filter((option) => option.value !== 'all')
          .map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
      </SelectField>
      <TextareaField
        error={localError ?? undefined}
        helpText="Shown in the account-status notification when provided."
        label="Reason"
        onChange={(event) => setReason(event.target.value)}
        value={reason}
      />
      <Button isLoading={updateStatus.isPending} loadingLabel="Saving" type="submit">
        Save status
      </Button>
    </form>
  )
}

function AdminUserDetail({
  onSaved,
  userId,
}: {
  onSaved: (user: AdminUserRDO) => void
  userId?: BackendId
}) {
  const detailQuery = useAdminUser(userId)
  const user = detailQuery.data

  if (detailQuery.isLoading) {
    return <p className="status">Loading user...</p>
  }

  if (detailQuery.isError || !user) {
    return (
      <Alert title="User could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  return (
    <div className="feature-stack">
      <DescriptionList
        items={[
          { label: 'User id', value: user.id },
          { label: 'Email', value: user.email },
          { label: 'Full name', value: user.fullName },
          { label: 'Phone', value: user.phone ?? 'Not provided' },
          { label: 'Status', value: humanizeToken(user.status) },
          { label: 'Roles', value: user.roles.map((role) => humanizeToken(role)).join(', ') },
          { label: 'Teacher status', value: humanizeToken(user.teacherStatus) },
          { label: 'Language', value: user.preferredLanguage ?? 'Not set' },
          { label: 'Created', value: formatDate(user.createdAt) },
          { label: 'Updated', value: formatDate(user.updatedAt) },
          { label: 'Deleted', value: formatDate(user.deletedAt) },
        ]}
      />
      <Panel title="Account status">
        <div className="feature-panel-body">
          <UserStatusForm onSaved={onSaved} user={user} />
        </div>
      </Panel>
    </div>
  )
}

export function AdminUsersPage() {
  const {
    clearFilters,
    filters,
    roleValue,
    statusValue,
    teacherStatusValue,
    updateFilters,
  } = useAdminUserFilters()
  const usersQuery = useAdminUsers(filters)
  const [selectedUserId, setSelectedUserId] = useState<BackendId | undefined>()
  const [statusMessage, setStatusMessage] = useState<string | null>(null)
  const users = usersQuery.data?.items ?? []
  const page = usersQuery.data?.meta.page

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Users" />

      {statusMessage && (
        <Alert title="User updated" variant="success">
          {statusMessage}
        </Alert>
      )}

      <Panel title="User directory" description="Search accounts, roles, and teacher status.">
        <FilterBar onClear={clearFilters}>
          <TextField
            label="Email search"
            onChange={(event) => updateFilters({ page: 0, q: event.target.value })}
            placeholder="student@example.com"
            value={filters.q ?? ''}
          />
          <SelectField
            label="Status"
            onChange={(event) => updateFilters({ page: 0, status: event.target.value })}
            value={statusValue}
          >
            {statusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Role"
            onChange={(event) => updateFilters({ page: 0, role: event.target.value })}
            value={roleValue}
          >
            {roleOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Teacher status"
            onChange={(event) => updateFilters({ page: 0, teacherStatus: event.target.value })}
            value={teacherStatusValue}
          >
            {teacherStatusOptions.map((option) => (
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

        <DataTable<AdminUserRDO>
          columns={[
            {
              header: 'User',
              key: 'user',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>{row.fullName}</strong>
                  <span>{row.email}</span>
                </div>
              ),
            },
            {
              header: 'Status',
              key: 'status',
              render: (row) => <Badge tone={statusTone(row.status)}>{humanizeToken(row.status)}</Badge>,
            },
            {
              header: 'Roles',
              key: 'roles',
              render: (row) => row.roles.map((role) => humanizeToken(role)).join(', '),
            },
            {
              header: 'Teacher status',
              key: 'teacherStatus',
              render: (row) => humanizeToken(row.teacherStatus, 'None'),
            },
            { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
            {
              header: 'Actions',
              key: 'actions',
              render: (row) => (
                <Button onClick={() => setSelectedUserId(row.id)} size="sm" variant="secondary">
                  Manage
                </Button>
              ),
            },
          ]}
          emptyMessage="No users match these filters."
          emptyTitle="No users"
          error={usersQuery.isError ? normalizeApiError(usersQuery.error).message : undefined}
          getRowKey={(row) => row.id}
          isLoading={usersQuery.isLoading}
          rows={users}
        />

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={usersQuery.isFetching}
            onNext={() => updateFilters({ page: page.number + 1 })}
            onPrevious={() => updateFilters({ page: Math.max(0, page.number - 1) })}
            page={page.number + 1}
            pageSize={page.size}
            totalItems={page.totalElements}
            totalPages={page.totalPages}
          />
        )}
      </Panel>

      <Dialog
        description="Review account metadata and change persisted access status."
        isOpen={Boolean(selectedUserId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedUserId(undefined)
          }
        }}
        title="Manage user"
      >
        <AdminUserDetail
          onSaved={(user) => setStatusMessage(`${user.fullName} is now ${humanizeToken(user.status)}.`)}
          userId={selectedUserId}
        />
      </Dialog>
    </PageContainer>
  )
}
