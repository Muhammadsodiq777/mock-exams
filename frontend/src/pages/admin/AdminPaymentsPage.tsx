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
  Tabs,
  TextareaField,
  TextField,
} from '../../components/ui'
import {
  useAdminPayment,
  useAdminPaymentDispute,
  useAdminPaymentDisputeDecision,
  useAdminPaymentDisputes,
  useAdminPayments,
} from '../../hooks/useAdminFinance'
import {
  useAdminScheduleBookings,
  useAdminScheduleProviderFailures,
} from '../../hooks/useLiveSpeakingSchedule'
import type {
  AdminPaymentDisputeListRequest,
  AdminPaymentDisputeRDO,
  AdminPaymentListRequest,
  AdminPaymentRDO,
  BackendId,
  LiveSpeakingBookingRDO,
  LiveSpeakingProviderFailureRDO,
  PaymentDisputeStatus,
  PaymentStatus,
} from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const pageSizeOptions = [10, 20, 50, 100]
const resolutionFields = ['resolutionNote', 'status'] as const

type PaymentStatusFilter = PaymentStatus | 'all'
type DisputeStatusFilter = PaymentDisputeStatus | 'all'

const paymentStatusOptions: Array<{ label: string; value: PaymentStatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Pending', value: 'pending' },
  { label: 'Paid', value: 'paid' },
  { label: 'Failed', value: 'failed' },
  { label: 'Cancelled', value: 'cancelled' },
  { label: 'Refunded', value: 'refunded' },
]

const disputeStatusOptions: Array<{ label: string; value: DisputeStatusFilter }> = [
  { label: 'All statuses', value: 'all' },
  { label: 'Open', value: 'open' },
  { label: 'Under review', value: 'under_review' },
  { label: 'Refunded', value: 'refunded' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Resolved', value: 'resolved' },
]

const decisionStatusOptions: Array<{ label: string; value: Exclude<PaymentDisputeStatus, 'open'> }> = [
  { label: 'Under review', value: 'under_review' },
  { label: 'Resolved', value: 'resolved' },
  { label: 'Rejected', value: 'rejected' },
  { label: 'Refunded', value: 'refunded' },
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

const statusTone = (status: string) => {
  if (status === 'paid' || status === 'resolved' || status === 'booked' || status === 'completed') return 'success'
  if (status === 'pending' || status === 'open' || status === 'under_review' || status === 'reserved') return 'warning'
  if (status === 'failed' || status === 'rejected' || status === 'cancelled' || status === 'no_show') return 'danger'
  return 'neutral'
}

function usePaymentFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const payments = useMemo<AdminPaymentListRequest>(() => ({
    page: numberParam(searchParams, 'page', 0),
    size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
    status: firstParam(searchParams, 'status') as PaymentStatus | undefined,
  }), [searchParams])

  const disputes = useMemo<AdminPaymentDisputeListRequest>(() => ({
    from: dayStart(firstParam(searchParams, 'from')),
    page: numberParam(searchParams, 'dpage', 0),
    paymentStatus: firstParam(searchParams, 'paymentStatus') as PaymentStatus | undefined,
    reason: firstParam(searchParams, 'reason'),
    size: numberParam(searchParams, 'dsize', DEFAULT_PAGE_SIZE),
    status: firstParam(searchParams, 'disputeStatus') as PaymentDisputeStatus | undefined,
    studentUserId: firstParam(searchParams, 'studentUserId')
      ? Number(firstParam(searchParams, 'studentUserId'))
      : undefined,
    teacherProfileId: firstParam(searchParams, 'teacherProfileId')
      ? Number(firstParam(searchParams, 'teacherProfileId'))
      : undefined,
    to: dayEnd(firstParam(searchParams, 'to')),
  }), [searchParams])

  const updateFilters = (updates: Record<string, number | string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(updates).forEach(([key, value]) => setParam(next, key, value))
      return next
    })
  }

  const clearFilters = (scope: 'disputes' | 'payments') => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      const keys =
        scope === 'payments'
          ? ['page', 'size', 'status']
          : [
              'dpage',
              'dsize',
              'disputeStatus',
              'from',
              'paymentStatus',
              'reason',
              'studentUserId',
              'teacherProfileId',
              'to',
            ]
      keys.forEach((key) => next.delete(key))
      return next
    })
  }

  return {
    clearFilters,
    disputes,
    from: dateInputValue(firstParam(searchParams, 'from')),
    payments,
    tab: firstParam(searchParams, 'tab') ?? 'payments',
    to: dateInputValue(firstParam(searchParams, 'to')),
    updateFilters,
  }
}

function PaymentDetail({ paymentId }: { paymentId?: BackendId }) {
  const detailQuery = useAdminPayment(paymentId)
  const payment = detailQuery.data

  if (detailQuery.isLoading) {
    return <p className="status">Loading payment...</p>
  }

  if (detailQuery.isError || !payment) {
    return (
      <Alert title="Payment could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  return (
    <div className="feature-stack">
      <DescriptionList
        items={[
          { label: 'Payment id', value: payment.id },
          { label: 'Student user id', value: payment.studentUserId },
          { label: 'Status', value: humanizeToken(payment.status) },
          { label: 'Provider', value: payment.provider },
          { label: 'Provider payment id', value: payment.providerPaymentId ?? 'Not provided' },
          { label: 'Amount', value: formatCurrency(payment.amount, payment.currency, '0 UZS') },
          { label: 'Target type', value: humanizeToken(payment.targetType) },
          { label: 'Listing', value: payment.targetListingTitle ?? payment.targetListingId ?? 'None' },
          { label: 'Live speaking booking', value: payment.targetLiveSpeakingBookingId ?? 'None' },
          { label: 'Created', value: formatDate(payment.createdAt) },
          { label: 'Paid', value: formatDate(payment.paidAt) },
          { label: 'Confirmation dispatched', value: formatDate(payment.confirmationDispatchedAt) },
        ]}
      />
      {(payment.failureCode || payment.failureMessage) && (
        <Alert title="Provider failure" variant="warning">
          {[payment.failureCode, payment.failureMessage].filter(Boolean).join(': ')}
        </Alert>
      )}
      {payment.promoCode && (
        <DescriptionList
          items={[
            { label: 'Promo code', value: payment.promoCode },
            { label: 'Original amount', value: formatCurrency(payment.promoOriginalAmount, payment.currency) },
            { label: 'Discount', value: formatCurrency(payment.promoDiscountAmount, payment.currency, '0 UZS') },
          ]}
        />
      )}
    </div>
  )
}

function DisputeDecisionForm({
  dispute,
  onDecision,
}: {
  dispute: AdminPaymentDisputeRDO
  onDecision: (message: string) => void
}) {
  const decision = useAdminPaymentDisputeDecision()
  const [status, setStatus] = useState<Exclude<PaymentDisputeStatus, 'open'>>(
    dispute.status === 'under_review' ? 'resolved' : 'under_review',
  )
  const [resolutionNote, setResolutionNote] = useState(dispute.resolutionNote ?? '')
  const [localError, setLocalError] = useState<string | null>(null)
  const serverError = decision.error
    ? toFormErrorState(decision.error, resolutionFields, 'Unable to save dispute decision.')
    : null

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const terminal = status !== 'under_review'
    const trimmed = resolutionNote.trim()
    if (terminal && !trimmed) {
      setLocalError('Resolution note is required for terminal decisions.')
      return
    }

    setLocalError(null)
    decision.mutate(
      {
        disputeId: dispute.id,
        payload: { resolutionNote: trimmed || null, status },
      },
      { onSuccess: () => onDecision(`Dispute ${dispute.id} moved to ${humanizeToken(status)}.`) },
    )
  }

  return (
    <form className="feature-stack" onSubmit={submit}>
      {serverError?.formMessage && (
        <Alert title="Decision failed" variant="danger">
          {serverError.formMessage}
        </Alert>
      )}
      <SelectField
        error={serverError?.fieldErrors.status}
        label="Decision status"
        onChange={(event) => setStatus(event.target.value as Exclude<PaymentDisputeStatus, 'open'>)}
        value={status}
      >
        {decisionStatusOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <TextareaField
        error={localError ?? serverError?.fieldErrors.resolutionNote}
        label="Resolution note"
        onChange={(event) => setResolutionNote(event.target.value)}
        required={status !== 'under_review'}
        value={resolutionNote}
      />
      <Button isLoading={decision.isPending} loadingLabel="Saving" type="submit">
        Save decision
      </Button>
    </form>
  )
}

function DisputeDetail({
  disputeId,
  onDecision,
}: {
  disputeId?: BackendId
  onDecision: (message: string) => void
}) {
  const detailQuery = useAdminPaymentDispute(disputeId)
  const dispute = detailQuery.data

  if (detailQuery.isLoading) {
    return <p className="status">Loading dispute...</p>
  }

  if (detailQuery.isError || !dispute) {
    return (
      <Alert title="Dispute could not be loaded" variant="danger">
        {normalizeApiError(detailQuery.error).message}
      </Alert>
    )
  }

  return (
    <div className="feature-stack">
      <DescriptionList
        items={[
          { label: 'Dispute id', value: dispute.id },
          { label: 'Payment id', value: dispute.paymentId },
          { label: 'Status', value: humanizeToken(String(dispute.status)) },
          {
            label: 'Payment amount',
            value: formatCurrency(dispute.paymentAmount, dispute.paymentCurrency, '0 UZS'),
          },
          { label: 'Student', value: dispute.studentDisplayName ?? dispute.studentUserId },
          { label: 'Teacher', value: dispute.teacherDisplayName ?? dispute.teacherProfileId ?? 'None' },
          { label: 'Target', value: humanizeToken(String(dispute.paymentTargetType)) },
          { label: 'Live speaking booking', value: dispute.paymentTargetLiveSpeakingBookingId ?? 'None' },
          { label: 'Created', value: formatDate(dispute.createdAt) },
          { label: 'Reviewed', value: formatDate(dispute.reviewedAt) },
        ]}
      />
      <Alert title={dispute.reason} variant="info">
        {dispute.description || 'No student description provided.'}
      </Alert>
      {dispute.resolutionNote && (
        <Alert title="Current resolution note" variant="success">
          {dispute.resolutionNote}
        </Alert>
      )}
      {!['refunded', 'rejected', 'resolved'].includes(String(dispute.status)) && (
        <DisputeDecisionForm dispute={dispute} onDecision={onDecision} />
      )}
    </div>
  )
}

function PaymentsTab({
  filters,
  onSelect,
  updateFilters,
}: {
  filters: AdminPaymentListRequest
  onSelect: (id: BackendId) => void
  updateFilters: (updates: Record<string, number | string | undefined>) => void
}) {
  const paymentsQuery = useAdminPayments(filters)
  const page = paymentsQuery.data?.meta.page

  return (
    <Panel title="Payment support" description="Read-only payment status and safe provider metadata.">
      <FilterBar onClear={() => updateFilters({ page: undefined, size: undefined, status: undefined })}>
        <SelectField
          label="Status"
          onChange={(event) => updateFilters({ page: 0, status: event.target.value })}
          value={filters.status ?? 'all'}
        >
          {paymentStatusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </SelectField>
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

      <DataTable<AdminPaymentRDO>
        columns={[
          {
            header: 'Payment',
            key: 'payment',
            render: (row) => (
              <div className="profile-table-stack">
                <strong>#{row.id}</strong>
                <span>{humanizeToken(row.targetType)}</span>
              </div>
            ),
          },
          {
            header: 'Status',
            key: 'status',
            render: (row) => <Badge tone={statusTone(row.status)}>{humanizeToken(row.status)}</Badge>,
          },
          { header: 'Provider', key: 'provider', render: (row) => row.provider },
          { header: 'Amount', key: 'amount', render: (row) => formatCurrency(row.amount, row.currency, '0 UZS') },
          { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
          {
            header: 'Actions',
            key: 'actions',
            render: (row) => (
              <Button onClick={() => onSelect(row.id)} size="sm" variant="secondary">
                Detail
              </Button>
            ),
          },
        ]}
        emptyMessage="No payments match the selected filters."
        emptyTitle="No payments"
        error={paymentsQuery.isError ? normalizeApiError(paymentsQuery.error).message : undefined}
        getRowKey={(row) => row.id}
        isLoading={paymentsQuery.isLoading}
        rows={paymentsQuery.data?.items ?? []}
      />

      {page && (
        <Pagination
          canGoNext={!page.last}
          canGoPrevious={!page.first}
          isLoading={paymentsQuery.isFetching}
          onNext={() => updateFilters({ page: (filters.page ?? 0) + 1 })}
          onPrevious={() => updateFilters({ page: Math.max((filters.page ?? 0) - 1, 0) })}
          page={page.number + 1}
          pageSize={page.size}
          totalItems={page.totalElements}
          totalPages={page.totalPages}
        />
      )}
    </Panel>
  )
}

function DisputesTab({
  filters,
  from,
  onSelect,
  to,
  updateFilters,
}: {
  filters: AdminPaymentDisputeListRequest
  from: string
  onSelect: (id: BackendId) => void
  to: string
  updateFilters: (updates: Record<string, number | string | undefined>) => void
}) {
  const disputesQuery = useAdminPaymentDisputes(filters)
  const page = disputesQuery.data?.meta.page

  return (
    <Panel title="Payment disputes" description="Review student disputes without changing payment state.">
      <FilterBar
        onClear={() =>
          updateFilters({
            disputeStatus: undefined,
            dpage: undefined,
            dsize: undefined,
            from: undefined,
            paymentStatus: undefined,
            reason: undefined,
            studentUserId: undefined,
            teacherProfileId: undefined,
            to: undefined,
          })
        }
      >
        <SelectField
          label="Dispute status"
          onChange={(event) => updateFilters({ disputeStatus: event.target.value, dpage: 0 })}
          value={filters.status ?? 'all'}
        >
          {disputeStatusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </SelectField>
        <SelectField
          label="Payment status"
          onChange={(event) => updateFilters({ dpage: 0, paymentStatus: event.target.value })}
          value={filters.paymentStatus ?? 'all'}
        >
          {paymentStatusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </SelectField>
        <TextField
          label="Reason"
          onChange={(event) => updateFilters({ dpage: 0, reason: event.target.value })}
          value={filters.reason ?? ''}
        />
        <TextField
          label="Teacher profile id"
          min={0}
          onChange={(event) => updateFilters({ dpage: 0, teacherProfileId: event.target.value })}
          type="number"
          value={filters.teacherProfileId ?? ''}
        />
        <TextField
          label="Student user id"
          min={0}
          onChange={(event) => updateFilters({ dpage: 0, studentUserId: event.target.value })}
          type="number"
          value={filters.studentUserId ?? ''}
        />
        <TextField
          label="From"
          onChange={(event) => updateFilters({ dpage: 0, from: event.target.value })}
          type="date"
          value={from}
        />
        <TextField
          label="To"
          onChange={(event) => updateFilters({ dpage: 0, to: event.target.value })}
          type="date"
          value={to}
        />
        <SelectField
          label="Page size"
          onChange={(event) => updateFilters({ dpage: 0, dsize: Number(event.target.value) })}
          value={filters.size ?? DEFAULT_PAGE_SIZE}
        >
          {pageSizeOptions.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </SelectField>
      </FilterBar>

      <DataTable<AdminPaymentDisputeRDO>
        columns={[
          {
            header: 'Dispute',
            key: 'dispute',
            render: (row) => (
              <div className="profile-table-stack">
                <strong>{row.reason}</strong>
                <span>Payment #{row.paymentId}</span>
              </div>
            ),
          },
          {
            header: 'Status',
            key: 'status',
            render: (row) => <Badge tone={statusTone(String(row.status))}>{humanizeToken(String(row.status))}</Badge>,
          },
          {
            header: 'Amount',
            key: 'amount',
            render: (row) => formatCurrency(row.paymentAmount, row.paymentCurrency, '0 UZS'),
          },
          { header: 'Student', key: 'student', render: (row) => row.studentDisplayName ?? row.studentUserId },
          { header: 'Teacher', key: 'teacher', render: (row) => row.teacherDisplayName ?? 'None' },
          { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
          {
            header: 'Actions',
            key: 'actions',
            render: (row) => (
              <Button onClick={() => onSelect(row.id)} size="sm" variant="secondary">
                Review
              </Button>
            ),
          },
        ]}
        emptyMessage="No disputes match the selected filters."
        emptyTitle="No disputes"
        error={disputesQuery.isError ? normalizeApiError(disputesQuery.error).message : undefined}
        getRowKey={(row) => row.id}
        isLoading={disputesQuery.isLoading}
        rows={disputesQuery.data?.items ?? []}
      />

      {page && (
        <Pagination
          canGoNext={!page.last}
          canGoPrevious={!page.first}
          isLoading={disputesQuery.isFetching}
          onNext={() => updateFilters({ dpage: (filters.page ?? 0) + 1 })}
          onPrevious={() => updateFilters({ dpage: Math.max((filters.page ?? 0) - 1, 0) })}
          page={page.number + 1}
          pageSize={page.size}
          totalItems={page.totalElements}
          totalPages={page.totalPages}
        />
      )}
    </Panel>
  )
}

function LiveSpeakingTab() {
  const bookingsQuery = useAdminScheduleBookings({ page: 0, size: DEFAULT_PAGE_SIZE, sort: 'startsAt,desc' })
  const failuresQuery = useAdminScheduleProviderFailures({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    sort: 'createdAt,desc',
  })

  return (
    <div className="feature-stack">
      <Panel
        title="Live speaking bookings"
        description="Support view for student reservations, payment state, and Meet link status."
      >
        <DataTable<LiveSpeakingBookingRDO>
          columns={[
            {
              header: 'Booking',
              key: 'booking',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>#{row.id}</strong>
                  <span>Slot #{row.slotId}</span>
                </div>
              ),
            },
            {
              header: 'Status',
              key: 'status',
              render: (row) => <Badge tone={statusTone(row.status)}>{humanizeToken(row.status)}</Badge>,
            },
            { header: 'Student', key: 'student', render: (row) => row.studentDisplayName ?? row.studentUserId },
            { header: 'Teacher', key: 'teacher', render: (row) => row.teacherDisplayName ?? row.teacherProfileId },
            {
              header: 'Payment',
              key: 'payment',
              render: (row) => row.paymentStatus ? humanizeToken(String(row.paymentStatus)) : row.paymentId ?? 'None',
            },
            {
              header: 'Provider',
              key: 'provider',
              render: (row) => humanizeToken(String(row.providerSyncStatus ?? 'pending')),
            },
            { header: 'Starts', key: 'startsAt', render: (row) => formatDate(row.startsAt) },
          ]}
          emptyMessage="No live speaking bookings are available yet."
          emptyTitle="No bookings"
          error={bookingsQuery.isError ? normalizeApiError(bookingsQuery.error).message : undefined}
          getRowKey={(row) => row.id}
          isLoading={bookingsQuery.isLoading}
          rows={bookingsQuery.data?.items ?? []}
        />
      </Panel>

      <Panel
        title="Provider failures"
        description="Google Calendar and Meet sync failures for live speaking support."
      >
        <DataTable<LiveSpeakingProviderFailureRDO>
          columns={[
            {
              header: 'Failure',
              key: 'failure',
              render: (row) => (
                <div className="profile-table-stack">
                  <strong>{row.failureCode ?? 'Provider failure'}</strong>
                  <span>Booking {row.bookingId ?? 'Not linked'}</span>
                </div>
              ),
            },
            {
              header: 'Status',
              key: 'status',
              render: (row) => (
                <Badge tone={statusTone(String(row.providerSyncStatus ?? 'failed'))}>
                  {humanizeToken(String(row.providerSyncStatus ?? 'failed'))}
                </Badge>
              ),
            },
            { header: 'Teacher', key: 'teacher', render: (row) => row.teacherDisplayName ?? row.teacherProfileId ?? 'None' },
            { header: 'Student', key: 'student', render: (row) => row.studentDisplayName ?? row.studentUserId ?? 'None' },
            { header: 'Retries', key: 'retries', render: (row) => row.retryCount ?? 0 },
            { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
          ]}
          emptyMessage="No live speaking provider failures are currently reported."
          emptyTitle="No provider failures"
          error={failuresQuery.isError ? normalizeApiError(failuresQuery.error).message : undefined}
          getRowKey={(row) => row.id}
          isLoading={failuresQuery.isLoading}
          rows={failuresQuery.data?.items ?? []}
        />
      </Panel>
    </div>
  )
}

export function AdminPaymentsPage() {
  const { disputes, from, payments, tab, to, updateFilters } = usePaymentFilters()
  const [selectedPaymentId, setSelectedPaymentId] = useState<BackendId | undefined>()
  const [selectedDisputeId, setSelectedDisputeId] = useState<BackendId | undefined>()
  const [message, setMessage] = useState<string | null>(null)
  const activeTab = tab === 'disputes' || tab === 'live-speaking' ? tab : 'payments'

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Payments" />

      {message && (
        <Alert title="Decision saved" variant="success">
          {message}
        </Alert>
      )}

      <Tabs
        onValueChange={(value) => updateFilters({ tab: value })}
        value={activeTab}
        items={[
          {
            content: (
              <PaymentsTab filters={payments} onSelect={setSelectedPaymentId} updateFilters={updateFilters} />
            ),
            id: 'payments',
            label: 'Payments',
          },
          {
            content: (
              <DisputesTab
                filters={disputes}
                from={from}
                onSelect={setSelectedDisputeId}
                to={to}
                updateFilters={updateFilters}
              />
            ),
            id: 'disputes',
            label: 'Disputes',
          },
          {
            content: <LiveSpeakingTab />,
            id: 'live-speaking',
            label: 'Live speaking',
          },
        ]}
      />

      <Dialog
        description="Safe payment metadata and purchase target context."
        isOpen={Boolean(selectedPaymentId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedPaymentId(undefined)
          }
        }}
        title="Payment detail"
      >
        <PaymentDetail paymentId={selectedPaymentId} />
      </Dialog>

      <Dialog
        description="Record a support decision. Terminal decisions require a resolution note."
        isOpen={Boolean(selectedDisputeId)}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedDisputeId(undefined)
          }
        }}
        title="Dispute review"
      >
        <DisputeDetail
          disputeId={selectedDisputeId}
          onDecision={(nextMessage) => {
            setMessage(nextMessage)
            setSelectedDisputeId(undefined)
          }}
        />
      </Dialog>
    </PageContainer>
  )
}
