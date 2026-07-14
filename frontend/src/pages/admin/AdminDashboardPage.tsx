import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  DescriptionList,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  Skeleton,
  TextField,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useAdminDashboard } from '../../hooks/useAdminDashboard'
import type { AdminDashboardRDO, DateRangeFilter } from '../../types/backend'
import { formatCurrency, formatDate, formatNumber, humanizeToken } from '../../utils/formatters'
import { normalizeApiError } from '../../utils/apiErrors'

const dayMs = 24 * 60 * 60 * 1000

const toDateInputValue = (date: Date) => date.toISOString().slice(0, 10)

const fromDateInputParam = (value: string | null, fallback: string) =>
  value && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : fallback

const startOfDayIso = (dateValue: string) => `${dateValue}T00:00:00.000Z`

const endOfDayIso = (dateValue: string) => `${dateValue}T23:59:59.999Z`

const rangeIsInvalid = (from: string, to: string) =>
  new Date(endOfDayIso(to)).getTime() < new Date(startOfDayIso(from)).getTime()

const count = (value?: number | null) => formatNumber(value ?? 0, '0')

const queueSeverity = (value: number) => (value > 0 ? 'warning' : 'success')

function useDashboardRange() {
  const [searchParams, setSearchParams] = useSearchParams()

  const defaults = useMemo(() => {
    const today = new Date()
    const from = new Date(today.getTime() - 29 * dayMs)

    return {
      from: toDateInputValue(from),
      to: toDateInputValue(today),
    }
  }, [])

  const from = fromDateInputParam(searchParams.get('from'), defaults.from)
  const to = fromDateInputParam(searchParams.get('to'), defaults.to)

  const updateRange = (updates: Partial<Record<'from' | 'to', string>>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      const nextFrom = updates.from ?? from
      const nextTo = updates.to ?? to

      next.set('from', nextFrom)
      next.set('to', nextTo)

      return next
    })
  }

  const resetRange = () => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('from')
      next.delete('to')
      return next
    })
  }

  return { from, invalid: rangeIsInvalid(from, to), resetRange, to, updateRange }
}

function DashboardSkeleton() {
  return (
    <div className="feature-card-grid">
      {Array.from({ length: 6 }, (_, index) => (
        <RepeatedItemCard key={index}>
          <Skeleton lines={4} />
        </RepeatedItemCard>
      ))}
    </div>
  )
}

function MetricCard({
  badge,
  description,
  detail,
  title,
  to,
  tone = 'info',
  value,
}: {
  badge: string
  description?: string
  detail?: string
  title: string
  to: string
  tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'info'
  value: string
}) {
  return (
    <RepeatedItemCard>
      <Badge tone={tone}>{badge}</Badge>
      <h3>{title}</h3>
      <p className="dashboard-metric">{value}</p>
      {detail && <p className="feature-card__summary">{detail}</p>}
      {description && <p className="feature-card__note">{description}</p>}
      <div className="feature-card__actions">
        <Link className="feature-link" to={to}>
          Open
        </Link>
      </div>
    </RepeatedItemCard>
  )
}

function PlatformMetrics({ dashboard }: { dashboard: AdminDashboardRDO }) {
  return (
    <Panel title="Platform metrics" description="Current state plus windowed activity.">
      <div className="feature-card-grid">
        <MetricCard
          badge="Users"
          detail={`${count(dashboard.users.active)} active, ${count(dashboard.users.newInWindow)} new`}
          title="Total users"
          to={ROUTES.adminUsers}
          value={count(dashboard.users.total)}
        />
        <MetricCard
          badge="Teachers"
          detail={`${count(dashboard.teachers.approved)} approved, ${count(dashboard.teachers.pendingApproval)} pending`}
          title="Teacher profiles"
          to={ROUTES.adminTeachers}
          tone={dashboard.teachers.pendingApproval > 0 ? 'warning' : 'success'}
          value={count(dashboard.teachers.total)}
        />
        <MetricCard
          badge="Tests"
          detail={`${count(dashboard.tests.published)} published, ${count(dashboard.tests.submitted)} submitted`}
          title="Content library"
          to={ROUTES.adminModeration}
          tone={dashboard.tests.submitted > 0 ? 'warning' : 'info'}
          value={count(dashboard.tests.total)}
        />
        <MetricCard
          badge="Payments"
          detail={`${count(dashboard.payments.failed)} failed, ${count(dashboard.payments.amountMismatch)} mismatched`}
          title="Paid amount"
          to={ROUTES.adminPayments}
          tone={dashboard.payments.failed + dashboard.payments.amountMismatch > 0 ? 'danger' : 'success'}
          value={formatCurrency(dashboard.payments.paidAmountTotal, 'UZS', '0 UZS')}
        />
        <MetricCard
          badge="Withdrawals"
          detail={`${count(dashboard.withdrawals.requested)} requested, ${count(dashboard.withdrawals.paid)} paid`}
          title="Requested amount"
          to={ROUTES.adminWithdrawals}
          tone={dashboard.withdrawals.requested > 0 ? 'warning' : 'success'}
          value={formatCurrency(dashboard.withdrawals.requestedAmountTotal, 'UZS', '0 UZS')}
        />
        <MetricCard
          badge="AI"
          detail={`${count(dashboard.aiUsage.failedRequests)} failed, ${count(dashboard.aiUsage.blockedRequests)} blocked`}
          title="AI requests"
          to={ROUTES.adminReports}
          tone={dashboard.aiUsage.failedRequests > 0 ? 'warning' : 'info'}
          value={count(dashboard.aiUsage.totalRequests)}
        />
      </div>
    </Panel>
  )
}

function QueueSection({ dashboard }: { dashboard: AdminDashboardRDO }) {
  const queues = [
    {
      label: 'Teacher approvals',
      to: `${ROUTES.adminTeachers}?status=pending_approval`,
      value: dashboard.queues.pendingTeacherApprovals,
    },
    {
      label: 'Test moderation',
      to: ROUTES.adminModeration,
      value: dashboard.queues.pendingTestModeration,
    },
    {
      label: 'Withdrawals',
      to: ROUTES.adminWithdrawals,
      value: dashboard.queues.pendingWithdrawals,
    },
    {
      label: 'Failed payments',
      to: `${ROUTES.adminPayments}?status=failed`,
      value: dashboard.queues.failedPayments,
    },
    {
      label: 'Amount mismatches',
      to: `${ROUTES.adminPayments}?amountMismatch=true`,
      value: dashboard.queues.amountMismatchPayments,
    },
  ]

  return (
    <Panel title="Action queues" description="Counts reflect current platform state.">
      <div className="feature-card-grid feature-card-grid--compact">
        {queues.map((queue) => (
          <RepeatedItemCard key={queue.label}>
            <Badge tone={queueSeverity(queue.value)}>{queue.value > 0 ? 'Needs review' : 'Clear'}</Badge>
            <h3>{queue.label}</h3>
            <p className="dashboard-metric">{count(queue.value)}</p>
            <Link className="feature-link" to={queue.to}>
              Review
            </Link>
          </RepeatedItemCard>
        ))}
      </div>
    </Panel>
  )
}

function TrendSection({ dashboard }: { dashboard: AdminDashboardRDO }) {
  const points = [
    { label: 'New users', value: dashboard.users.newInWindow },
    { label: 'Paid payments', value: dashboard.payments.paid },
    { label: 'AI completed', value: dashboard.aiUsage.completedRequests },
    { label: 'Withdrawals', value: dashboard.withdrawals.requested },
  ]
  const max = Math.max(...points.map((point) => point.value), 1)

  return (
    <Panel title="Window snapshot" description="Small trend view for the selected date range.">
      <div className="dashboard-chart" aria-label="Admin dashboard selected-window snapshot">
        {points.map((point) => (
          <div className="dashboard-chart__bar" key={point.label}>
            <span style={{ height: `${Math.max((point.value / max) * 100, 6)}%` }} />
            <strong>{count(point.value)}</strong>
            <p>{point.label}</p>
          </div>
        ))}
      </div>
    </Panel>
  )
}

function RecentActivitySection({ dashboard }: { dashboard: AdminDashboardRDO }) {
  return (
    <Panel title="Recent activity" description="Latest non-sensitive operational events.">
      {dashboard.recentActivity.length === 0 ? (
        <div className="feature-panel-state">
          <EmptyState
            eyebrow="Quiet"
            title="No recent activity"
            message="The dashboard endpoint returned an empty recent activity list."
          />
        </div>
      ) : (
        <div className="feature-list feature-panel-body">
          {dashboard.recentActivity.map((activity) => (
            <RepeatedItemCard key={`${activity.type}-${activity.referenceId}-${activity.occurredAt}`}>
              <Badge tone="neutral">{humanizeToken(activity.type)}</Badge>
              <h3>{activity.summary}</h3>
              <DescriptionList
                items={[
                  { label: 'Reference', value: `#${activity.referenceId}` },
                  { label: 'Occurred', value: formatDate(activity.occurredAt) },
                ]}
              />
            </RepeatedItemCard>
          ))}
        </div>
      )}
    </Panel>
  )
}

export function AdminDashboardPage() {
  const range = useDashboardRange()
  const filters = useMemo<DateRangeFilter | undefined>(
    () =>
      range.invalid
        ? undefined
        : {
            from: startOfDayIso(range.from),
            to: endOfDayIso(range.to),
          },
    [range.from, range.invalid, range.to],
  )
  const dashboardQuery = useAdminDashboard(filters, !range.invalid)
  const dashboardResult = dashboardQuery.data
  const dashboard = dashboardResult?.dashboard
  const error = dashboardQuery.isError ? normalizeApiError(dashboardQuery.error) : null

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Admin"
        title="Dashboard"
        actions={
          <div className="admin-date-range" aria-label="Dashboard date range">
            <TextField
              error={range.invalid ? 'Start must be on or before end.' : undefined}
              label="From"
              onChange={(event) => range.updateRange({ from: event.target.value })}
              type="date"
              value={range.from}
            />
            <TextField
              error={range.invalid ? 'End must be on or after start.' : undefined}
              label="To"
              onChange={(event) => range.updateRange({ to: event.target.value })}
              type="date"
              value={range.to}
            />
            <Button onClick={range.resetRange} size="sm" variant="secondary">
              Reset
            </Button>
          </div>
        }
      />

      {range.invalid && (
        <Alert title="Invalid date range" variant="danger">
          Choose a start date that is earlier than or equal to the end date.
        </Alert>
      )}

      {dashboardQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading admin dashboard" />
            <DashboardSkeleton />
          </div>
        </Panel>
      )}

      {error && (
        <Alert title="Admin dashboard could not be loaded" variant="danger">
          <div className="feature-stack">
            <p>{error.message}</p>
            <Button
              onClick={() => {
                void dashboardQuery.refetch()
              }}
              size="sm"
              variant="secondary"
            >
              Retry
            </Button>
          </div>
        </Alert>
      )}

      {dashboard && (
        <div className="admin-dashboard-stack">
          <Alert title="Freshness" variant={dashboardResult.meta.stale ? 'warning' : 'info'}>
            As of {formatDate(dashboardResult.meta.asOf)}. Live speaking metrics are not shown
            until the backend dashboard contract exposes them.
          </Alert>
          <PlatformMetrics dashboard={dashboard} />
          <div className="dashboard-grid">
            <QueueSection dashboard={dashboard} />
            <TrendSection dashboard={dashboard} />
            <RecentActivitySection dashboard={dashboard} />
          </div>
        </div>
      )}
    </PageContainer>
  )
}
