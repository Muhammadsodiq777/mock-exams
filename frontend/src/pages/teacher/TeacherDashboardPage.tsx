import { Link } from 'react-router-dom'

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
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useTeacherDashboard } from '../../hooks/useTeacherDashboard'
import type {
  TeacherDashboardRDO,
  TeacherDashboardScheduleRDO,
  TeacherDashboardTestCountsRDO,
  TeacherProfileStatus,
} from '../../types/backend'
import { formatDate, formatNumber, humanizeToken } from '../../utils/formatters'

const profileStatusTone = (status: TeacherProfileStatus) => {
  if (status === 'approved') {
    return 'success'
  }

  if (status === 'rejected' || status === 'suspended') {
    return 'danger'
  }

  return 'warning'
}

const formatMoney = (
  amount?: number | string | null,
  currency = 'UZS',
  fallback = 'Not available',
) => {
  if (amount === undefined || amount === null || amount === '') {
    return fallback
  }

  const numeric = Number(amount)
  if (!Number.isFinite(numeric)) {
    return `${amount} ${currency}`
  }

  try {
    return new Intl.NumberFormat(undefined, {
      currency,
      maximumFractionDigits: 0,
      style: 'currency',
    }).format(numeric)
  } catch {
    return `${numeric.toLocaleString()} ${currency}`
  }
}

const formatLocalDateTime = (value?: string | null, fallback = 'Not scheduled') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.valueOf())) {
    return fallback
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

const countOrUnavailable = (value?: number | null) =>
  value === undefined || value === null ? 'Not available' : value

function dashboardHasActivity(dashboard: TeacherDashboardRDO) {
  const tests = dashboard.tests
  const wallet = dashboard.wallet

  return Boolean(
    (tests &&
      (tests.draft > 0 ||
        tests.submitted > 0 ||
        (tests.pendingReview ?? 0) > 0 ||
        tests.published > 0 ||
        tests.rejected > 0)) ||
      Number(wallet?.balanceAmount ?? 0) > 0 ||
      Number(wallet?.pendingAmount ?? 0) > 0 ||
      Number(dashboard.sales?.recentSalesCount ?? 0) > 0 ||
      Number(dashboard.attempts?.recentAttemptCount ?? 0) > 0 ||
      Number(dashboard.reviews?.ratingCount ?? 0) > 0 ||
      Number(dashboard.notifications?.unreadCount ?? 0) > 0 ||
      dashboard.schedule?.nextSession,
  )
}

function ApprovalBanner({ dashboard }: { dashboard: TeacherDashboardRDO }) {
  const profile = dashboard.profile
  const status = profile.status

  if (status === 'approved') {
    return (
      <Alert title="Teacher profile approved" variant="success">
        Your dashboard is ready. Keep an eye on moderation, student activity, earnings, and
        notifications from here.
      </Alert>
    )
  }

  if (status === 'rejected') {
    return (
      <Alert title="Teacher profile needs updates" variant="danger">
        <div className="feature-stack">
          <p>
            {profile.rejectionReason
              ? profile.rejectionReason
              : 'Review the profile notes, update your details, and resubmit when ready.'}
          </p>
          <Link className="feature-link feature-link--primary" to={ROUTES.teacherProfile}>
            Update profile
          </Link>
        </div>
      </Alert>
    )
  }

  if (status === 'pending_approval') {
    return (
      <Alert title="Profile waiting for approval" variant="warning">
        <div className="feature-stack">
          <p>
            Your profile is with the moderation team. Builder, earnings, schedule, and student
            tools may stay limited until approval is complete.
          </p>
          <Link className="feature-link" to={ROUTES.teacherProfile}>
            View profile
          </Link>
        </div>
      </Alert>
    )
  }

  return (
    <Alert title="Complete your teacher profile" variant="warning">
      <div className="feature-stack">
        <p>
          Add the missing profile details, then submit for approval to unlock teacher tools.
        </p>
        <Link className="feature-link feature-link--primary" to={ROUTES.teacherProfile}>
          Complete profile
        </Link>
      </div>
    </Alert>
  )
}

function ProfileSummaryCard({ dashboard }: { dashboard: TeacherDashboardRDO }) {
  const profile = dashboard.profile

  return (
    <RepeatedItemCard className="dashboard-band-card">
      <Badge tone={profileStatusTone(profile.status)}>{humanizeToken(profile.status)}</Badge>
      <p className="dashboard-band-card__score">
        {profile.displayName || 'Teacher'}
      </p>
      <p className="feature-card__summary">
        {profile.status === 'approved'
          ? 'Your operating snapshot is available below.'
          : 'Profile approval controls which teacher tools are currently available.'}
      </p>
      <DescriptionList
        items={[
          {
            label: 'Profile complete',
            value:
              profile.profileComplete === undefined || profile.profileComplete === null
                ? 'Not available'
                : profile.profileComplete
                  ? 'Yes'
                  : 'No',
          },
          { label: 'Updated', value: formatDate(profile.updatedAt) },
        ]}
      />
    </RepeatedItemCard>
  )
}

function TestCountCards({ tests }: { tests?: TeacherDashboardTestCountsRDO | null }) {
  const cards = [
    { label: 'Draft tests', value: tests?.draft, to: `${ROUTES.teacherTests}?status=draft` },
    {
      label: 'Submitted tests',
      value: tests?.submitted ?? tests?.pendingReview,
      to: `${ROUTES.teacherTests}?status=submitted`,
    },
    {
      label: 'Published tests',
      value: tests?.published,
      to: `${ROUTES.teacherTests}?status=published`,
    },
    {
      label: 'Rejected tests',
      value: tests?.rejected,
      to: `${ROUTES.teacherTests}?status=rejected`,
    },
  ]

  return (
    <div className="feature-card-grid">
      {cards.map((card) => (
        <RepeatedItemCard key={card.label}>
          <h3>{card.label}</h3>
          <p className="dashboard-metric">{countOrUnavailable(card.value)}</p>
          <Link className="feature-link" to={card.to}>
            Open tests
          </Link>
        </RepeatedItemCard>
      ))}
    </div>
  )
}

function FinanceCards({ dashboard }: { dashboard: TeacherDashboardRDO }) {
  const wallet = dashboard.wallet
  const sales = dashboard.sales
  const currency = wallet?.currency ?? sales?.currency ?? 'UZS'

  return (
    <div className="feature-card-grid feature-card-grid--compact">
      <RepeatedItemCard>
        <Badge tone="info">Wallet</Badge>
        <h3>Available balance</h3>
        <p className="dashboard-metric">
          {formatMoney(wallet?.availableAmount ?? wallet?.balanceAmount, currency)}
        </p>
        <DescriptionList
          items={[
            { label: 'Pending', value: formatMoney(wallet?.pendingAmount, currency) },
            { label: 'Currency', value: wallet?.currency ?? 'Not available' },
          ]}
        />
        <Link className="feature-link" to={ROUTES.teacherEarnings}>
          Open earnings
        </Link>
      </RepeatedItemCard>

      <RepeatedItemCard>
        <Badge tone="success">Sales</Badge>
        <h3>Recent sales</h3>
        <p className="dashboard-metric">{countOrUnavailable(sales?.recentSalesCount)}</p>
        <DescriptionList
          items={[
            {
              label: 'Recent revenue',
              value: formatMoney(sales?.recentSalesAmount, sales?.currency ?? currency),
            },
            { label: 'Latest sale', value: formatDate(sales?.latestSaleAt) },
          ]}
        />
        <Link className="feature-link" to={ROUTES.teacherEarnings}>
          Review sales
        </Link>
      </RepeatedItemCard>
    </div>
  )
}

function ActivityCards({ dashboard }: { dashboard: TeacherDashboardRDO }) {
  return (
    <div className="feature-card-grid">
      <RepeatedItemCard>
        <Badge tone="info">Attempts</Badge>
        <h3>Recent attempts</h3>
        <p className="dashboard-metric">
          {countOrUnavailable(dashboard.attempts?.recentAttemptCount)}
        </p>
        <p className="feature-card__summary">
          Latest: {formatDate(dashboard.attempts?.latestAttemptAt)}
        </p>
        <Link className="feature-link" to={ROUTES.teacherStudents}>
          Open students
        </Link>
      </RepeatedItemCard>

      <RepeatedItemCard>
        <Badge tone="success">Reviews</Badge>
        <h3>Reviews</h3>
        <p className="dashboard-metric">
          {formatNumber(dashboard.reviews?.averageRating, 'Not available')}
        </p>
        <DescriptionList
          items={[
            { label: 'Total', value: countOrUnavailable(dashboard.reviews?.ratingCount) },
            { label: 'Recent', value: countOrUnavailable(dashboard.reviews?.recentReviewCount) },
          ]}
        />
        <Link className="feature-link" to={ROUTES.teacherStudents}>
          Review activity
        </Link>
      </RepeatedItemCard>

      <RepeatedItemCard>
        <Badge tone={(dashboard.notifications?.unreadCount ?? 0) > 0 ? 'warning' : 'neutral'}>
          Notifications
        </Badge>
        <h3>Unread notifications</h3>
        <p className="dashboard-metric">
          {countOrUnavailable(dashboard.notifications?.unreadCount)}
        </p>
        <p className="feature-card__summary">
          Latest: {formatDate(dashboard.notifications?.latestNotificationAt)}
        </p>
        <Link className="feature-link" to={ROUTES.teacherNotifications}>
          Open notifications
        </Link>
      </RepeatedItemCard>
    </div>
  )
}

function ScheduleCard({ schedule }: { schedule?: TeacherDashboardScheduleRDO | null }) {
  if (!schedule?.enabled && !schedule?.nextSession) {
    return null
  }

  const session = schedule.nextSession

  return (
    <Panel
      title="Live speaking"
      description="Schedule data appears when the live speaking APIs are available."
    >
      <div className="feature-panel-body">
        {session ? (
          <RepeatedItemCard>
            <Badge tone={session.joinLinkAvailable || session.meetUrl ? 'success' : 'warning'}>
              {session.joinLinkAvailable || session.meetUrl ? 'Join link ready' : 'Join link pending'}
            </Badge>
            <h3>Next live speaking session</h3>
            <DescriptionList
              items={[
                { label: 'Starts', value: formatLocalDateTime(session.startsAt) },
                { label: 'Student', value: session.studentDisplayName ?? 'Not available' },
                { label: 'Status', value: humanizeToken(session.status) },
                {
                  label: 'Pending Meet links',
                  value: schedule.pendingMeetLinkCount ?? 'Not available',
                },
              ]}
            />
            <Link className="feature-link" to={ROUTES.teacherSchedule}>
              Open schedule
            </Link>
          </RepeatedItemCard>
        ) : (
          <EmptyState
            title="No scheduled session"
            message="Live speaking bookings will appear here when schedule APIs return data."
            actions={
              <Link className="feature-link" to={ROUTES.teacherSchedule}>
                Open schedule
              </Link>
            }
          />
        )}
      </div>
    </Panel>
  )
}

export function TeacherDashboardPage() {
  const dashboardQuery = useTeacherDashboard()
  const dashboard = dashboardQuery.data?.dashboard
  const approved = dashboard?.profile.status === 'approved'
  const fallbackDashboard = dashboard?.state === 'fallback'

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Teacher"
        title="Dashboard"
        actions={
          <>
            {approved && (
              <Link className="feature-link feature-link--primary" to={ROUTES.teacherTestCreate}>
                Create test
              </Link>
            )}
            <Link className="feature-link" to={ROUTES.teacherProfile}>
              Teacher profile
            </Link>
          </>
        }
      />

      {dashboardQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading teacher dashboard" />
            <Skeleton lines={5} />
          </div>
        </Panel>
      )}

      {dashboardQuery.isError && (
        <Panel>
          <div className="feature-panel-state">
            <Alert title="Teacher dashboard could not be loaded" variant="danger">
              {dashboardQuery.error instanceof Error
                ? dashboardQuery.error.message
                : 'Refresh the dashboard and try again.'}
            </Alert>
            <Button
              onClick={() => {
                void dashboardQuery.refetch()
              }}
              variant="secondary"
            >
              Retry
            </Button>
          </div>
        </Panel>
      )}

      {dashboard && (
        <>
          {fallbackDashboard && (
            <Alert title="Dashboard summary is partially available" variant="info">
              Some dashboard sections are using existing teacher APIs until the dedicated dashboard
              endpoint is available. Missing sections are shown as unavailable instead of estimated.
            </Alert>
          )}

          <ApprovalBanner dashboard={dashboard} />

          {!approved && (
            <div className="dashboard-grid">
              <section className="dashboard-grid__summary">
                <ProfileSummaryCard dashboard={dashboard} />
              </section>
              <Panel title="Next step" description="Profile approval unlocks the teacher workspace.">
                <EmptyState
                  className="feature-panel-state"
                  title={
                    dashboard.profile.status === 'pending_approval'
                      ? 'Wait for moderation'
                      : 'Prepare your profile'
                  }
                  message={
                    dashboard.profile.status === 'pending_approval'
                      ? 'You can review your submitted profile while the moderation team checks it.'
                      : 'Update your teacher profile and submit it for approval when it is complete.'
                  }
                  actions={
                    <Link className="feature-link feature-link--primary" to={ROUTES.teacherProfile}>
                      Open profile
                    </Link>
                  }
                />
              </Panel>
            </div>
          )}

          {approved && !fallbackDashboard && !dashboardHasActivity(dashboard) && (
            <Panel>
              <EmptyState
                className="feature-panel-state"
                eyebrow="New teacher"
                title="Start with your first test"
                message="Your dashboard has no sales, attempts, reviews, or notifications yet. Create a draft or open your owned tests to begin."
                actions={
                  <>
                    <Link className="feature-link feature-link--primary" to={ROUTES.teacherTestCreate}>
                      Create test
                    </Link>
                    <Link className="feature-link" to={ROUTES.teacherTests}>
                      Open tests
                    </Link>
                  </>
                }
              />
            </Panel>
          )}

          {approved && (
            <div className="dashboard-grid">
            <section className="dashboard-grid__summary">
              <ProfileSummaryCard dashboard={dashboard} />
            </section>

            <Panel title="Content" description="Owned test statuses from the dashboard summary.">
              <TestCountCards tests={dashboard.tests} />
            </Panel>

            <Panel title="Earnings" description="Wallet and sales totals from teacher-owned data.">
              <div className="feature-panel-body">
                <FinanceCards dashboard={dashboard} />
              </div>
            </Panel>

            <Panel title="Student activity" description="Recent attempts, reviews, and notifications.">
              <div className="feature-panel-body">
                <ActivityCards dashboard={dashboard} />
              </div>
            </Panel>

            <ScheduleCard schedule={dashboard.schedule} />
          </div>
          )}
        </>
      )}
    </PageContainer>
  )
}
