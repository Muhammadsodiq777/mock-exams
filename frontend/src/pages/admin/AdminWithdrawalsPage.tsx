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
  useAdminWithdrawal,
  useAdminWithdrawalDecisions,
  useAdminWithdrawals,
} from '../../hooks/useAdminFinance'
import type {
  AdminWithdrawalListRequest,
  AdminWithdrawalRequestRDO,
  BackendId,
  WithdrawalPayoutDetails,
  WithdrawalRequestStatus,
} from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const pageSizeOptions = [10, 20, 50, 100]
const reasonFields = ['reason'] as const

type StatusFilter = WithdrawalRequestStatus | 'all'

const statusOptions: Array<{ label: string; value: StatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Requested', value: 'requested' },
  { label: 'Approved', value: 'approved' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Paid', value: 'paid' },
  { label: 'Cancelled', value: 'cancelled' },
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

function useWithdrawalFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<AdminWithdrawalListRequest>(() => ({
    page: numberParam(searchParams, 'page', 0),
    size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
    status: firstParam(searchParams, 'status') as WithdrawalRequestStatus | undefined,
    teacherProfileId: firstParam(searchParams, 'teacherProfileId')
      ? Number(firstParam(searchParams, 'teacherProfileId'))
      : undefined,
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
      ;['page', 'size', 'status', 'teacherProfileId'].forEach((key) => next.delete(key))
      return next
    })
  }

  return { clearFilters, filters, updateFilters }
}

const statusTone = (status: string) => {
  if (status === 'paid' || status === 'approved') return 'success'
  if (status === 'requested') return 'warning'
  if (status === 'rejected' || status === 'cancelled') return 'danger'
  return 'neutral'
}

const payoutItems = (details: Partial<WithdrawalPayoutDetails> & { method: string }) => {
  if (details.method === 'card') {
    return [
      { label: 'Method', value: 'Card' },
      { label: 'Card number', value: details.cardNumber ?? 'Masked by backend' },
      { label: 'Card holder', value: details.cardHolder ?? 'Not provided' },
    ]
  }

  return [
    { label: 'Method', value: humanizeToken(details.method) },
    { label: 'Bank', value: details.bankName ?? 'Not provided' },
    { label: 'Account number', value: details.accountNumber ?? 'Masked by backend' },
    { label: 'Account holder', value: details.accountHolder ?? 'Not provided' },
  ]
}

function RejectionForm({
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
    ? toFormErrorState(error, reasonFields, 'Unable to reject withdrawal.')
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
        Reject withdrawal
      </Button>
    </form>
  )
}

function WithdrawalDetail({
  onDecision,
  withdrawalRequestId,
}: {
  onDecision: (message: string) => void
  withdrawalRequestId?: BackendId
}) {
  const detailQuery = useAdminWithdrawal(withdrawalRequestId)
  const decisions = useAdminWithdrawalDecisions()
  const withdrawal = detailQuery.data

  if (detailQuery.isLoading) {
    return <p className="status">Loading withdrawal...</p>
  }

  if (detailQuery.isError || !withdrawal) {
    return (
      <Alert title="Withdrawal could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  return (
    <div className="feature-stack">
      {(decisions.approve.isError || decisions.markPaid.isError) && (
        <Alert title="Decision failed" variant="danger">
          {normalizeApiError(decisions.approve.error ?? decisions.markPaid.error).message}
        </Alert>
      )}
      <DescriptionList
        items={[
          { label: 'Teacher', value: withdrawal.teacherDisplayName },
          { label: 'Teacher profile id', value: withdrawal.teacherProfileId },
          {
            label: 'Requested amount',
            value: formatCurrency(withdrawal.requestedAmount, withdrawal.currency, '0 UZS'),
          },
          { label: 'Status', value: humanizeToken(withdrawal.status) },
          { label: 'Requested', value: formatDate(withdrawal.createdAt) },
          { label: 'Reviewed', value: formatDate(withdrawal.reviewedAt) },
          { label: 'Paid', value: formatDate(withdrawal.paidAt) },
        ]}
      />
      <Panel title="Masked payout details">
        <div className="feature-panel-body">
          <DescriptionList items={payoutItems(withdrawal.payoutDetails)} />
        </div>
      </Panel>
      {withdrawal.rejectionReason && (
        <Alert title="Rejection reason" variant="warning">
          {withdrawal.rejectionReason}
        </Alert>
      )}
      <div className="teacher-approval-actions">
        {withdrawal.status === 'requested' && (
          <>
            <Button
              isLoading={decisions.approve.isPending}
              onClick={() =>
                decisions.approve.mutate(withdrawal.id, {
                  onSuccess: () => onDecision(`Withdrawal ${withdrawal.id} approved.`),
                })
              }
            >
              Approve withdrawal
            </Button>
            <RejectionForm
              error={decisions.reject.error}
              isLoading={decisions.reject.isPending}
              onSubmit={(reason) =>
                decisions.reject.mutate(
                  { payload: { reason }, withdrawalRequestId: withdrawal.id },
                  { onSuccess: () => onDecision(`Withdrawal ${withdrawal.id} rejected.`) },
                )
              }
            />
          </>
        )}
        {withdrawal.status === 'approved' && (
          <Button
            isLoading={decisions.markPaid.isPending}
            onClick={() =>
              decisions.markPaid.mutate(withdrawal.id, {
                onSuccess: () => onDecision(`Withdrawal ${withdrawal.id} marked paid.`),
              })
            }
          >
            Mark paid
          </Button>
        )}
      </div>
    </div>
  )
}

export function AdminWithdrawalsPage() {
  const { clearFilters, filters, updateFilters } = useWithdrawalFilters()
  const withdrawalsQuery = useAdminWithdrawals(filters)
  const [selectedId, setSelectedId] = useState<BackendId | undefined>()
  const [decisionMessage, setDecisionMessage] = useState<string | null>(null)
  const page = withdrawalsQuery.data?.meta.page

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Withdrawals" />

      {decisionMessage && (
        <Alert title="Decision saved" variant="success">
          {decisionMessage}
        </Alert>
      )}

      <Panel title="Payout review queue" description="Approve, reject, and mark manual payouts paid.">
        <FilterBar onClear={clearFilters}>
          <SelectField
            label="Status"
            onChange={(event) => updateFilters({ page: 0, status: event.target.value })}
            value={filters.status ?? 'all'}
          >
            {statusOptions.map((option) => (
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

        <DataTable<AdminWithdrawalRequestRDO>
          columns={[
            {
              header: 'Teacher',
              key: 'teacher',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>{row.teacherDisplayName}</strong>
                  <span>Profile #{row.teacherProfileId}</span>
                </div>
              ),
            },
            {
              header: 'Status',
              key: 'status',
              render: (row) => (
                <Badge tone={statusTone(row.status)}>{humanizeToken(row.status)}</Badge>
              ),
            },
            {
              header: 'Amount',
              key: 'amount',
              render: (row) => formatCurrency(row.requestedAmount, row.currency, '0 UZS'),
            },
            { header: 'Requested', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
            { header: 'Updated', key: 'updatedAt', render: (row) => formatDate(row.updatedAt) },
            {
              header: 'Actions',
              key: 'actions',
              render: (row) => (
                <Button onClick={() => setSelectedId(row.id)} size="sm" variant="secondary">
                  Review
                </Button>
              ),
            },
          ]}
          emptyMessage="No withdrawal requests match the selected filters."
          emptyTitle="No withdrawals"
          error={withdrawalsQuery.isError ? normalizeApiError(withdrawalsQuery.error).message : undefined}
          getRowKey={(row) => row.id}
          isLoading={withdrawalsQuery.isLoading}
          rows={withdrawalsQuery.data?.items ?? []}
        />

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={withdrawalsQuery.isFetching}
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
        description="Masked payout details and traceable admin status transitions."
        isOpen={Boolean(selectedId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedId(undefined)
          }
        }}
        title="Withdrawal review"
      >
        <WithdrawalDetail
          onDecision={(message) => {
            setDecisionMessage(message)
            setSelectedId(undefined)
          }}
          withdrawalRequestId={selectedId}
        />
      </Dialog>
    </PageContainer>
  )
}
