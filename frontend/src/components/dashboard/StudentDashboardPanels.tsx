import { Link } from 'react-router-dom'

import { fillRoute, ROUTES } from '../../constants/routes'
import type {
  AiUsageRemainingRDO,
  ProgressRecommendationRDO,
  RecentAttemptRDO,
  SkillScorePointRDO,
  StudentProgressDashboardRDO,
  StudyPlanRDO,
  StudyPlanSkill,
} from '../../types/backend'
import { formatDate, formatNumber, formatSeconds, humanizeToken } from '../../utils/formatters'
import { Badge, DescriptionList, EmptyState, RepeatedItemCard } from '../ui'

const skills: StudyPlanSkill[] = ['listening', 'reading', 'writing', 'speaking']

const latestPoint = (points?: SkillScorePointRDO[]) =>
  [...(points ?? [])].sort((a, b) => Date.parse(b.scoredAt) - Date.parse(a.scoredAt))[0]

function recommendationPath(recommendation: ProgressRecommendationRDO) {
  if (recommendation.testId) {
    return fillRoute(ROUTES.testDetail, { id: recommendation.testId })
  }

  return recommendation.sourceType.includes('marketplace') ? ROUTES.marketplace : ROUTES.tests
}

export function BandSummary({ dashboard }: { dashboard: StudentProgressDashboardRDO }) {
  const summary = dashboard.summary
  const overall = formatNumber(summary.overallBandEstimate)

  return (
    <RepeatedItemCard className="dashboard-band-card">
      <Badge tone={dashboard.state === 'empty' ? 'warning' : 'success'}>
        {humanizeToken(dashboard.state)}
      </Badge>
      <p className="dashboard-band-card__score">{overall}</p>
      <p className="feature-card__summary">
        {summary.completedAttemptCount > 0
          ? `${summary.completedAttemptCount} completed attempt${summary.completedAttemptCount === 1 ? '' : 's'}`
          : 'Start with a Reading or Listening test to build your estimate.'}
      </p>
      <DescriptionList
        items={[
          { label: 'Latest result', value: formatDate(summary.latestResultAt) },
          {
            label: 'Missing skills',
            value: summary.missingSkills.length
              ? summary.missingSkills.map((skill) => humanizeToken(skill)).join(', ')
              : 'None',
          },
        ]}
      />
    </RepeatedItemCard>
  )
}

export function SkillOverview({ dashboard }: { dashboard: StudentProgressDashboardRDO }) {
  return (
    <div className="dashboard-skill-grid">
      {skills.map((skill) => {
        const band = dashboard.summary.skillBands[skill]
        const missing = dashboard.summary.missingSkills.includes(skill)
        const point = latestPoint(dashboard.skillHistories[skill])

        return (
          <RepeatedItemCard key={skill}>
            <Badge tone={missing ? 'warning' : 'info'}>{humanizeToken(skill)}</Badge>
            <p className="dashboard-metric">{formatNumber(band, missing ? 'Practice needed' : 'Not measured')}</p>
            <p className="feature-card__summary">
              {point
                ? `${point.testTitle} scored ${formatNumber(point.estimatedBand)}`
                : 'No scored attempt in this window.'}
            </p>
          </RepeatedItemCard>
        )
      })}
    </div>
  )
}

export function ProgressChart({ dashboard }: { dashboard: StudentProgressDashboardRDO }) {
  const points = Object.entries(dashboard.skillHistories).flatMap(([skill, history]) =>
    (history ?? []).slice(-5).map((point) => ({ ...point, skill })),
  )

  if (points.length === 0) {
    return (
      <EmptyState
        title="No chart data yet"
        message="Completed Reading and Listening attempts will appear here once scored."
      />
    )
  }

  return (
    <div className="dashboard-chart" aria-label="Recent score chart">
      {points.map((point) => {
        const band = Number(point.estimatedBand ?? 0)
        const height = Number.isFinite(band) ? Math.max(8, (band / 9) * 100) : 8

        return (
          <div className="dashboard-chart__bar" key={`${point.skill}-${point.resultId}`}>
            <span style={{ height: `${height}%` }} />
            <p>{humanizeToken(point.skill)}</p>
            <strong>{formatNumber(point.estimatedBand, '-')}</strong>
          </div>
        )
      })}
    </div>
  )
}

export function WeakAreas({ dashboard }: { dashboard: StudentProgressDashboardRDO }) {
  if (dashboard.weakAreas.length === 0) {
    return <EmptyState title="No weak areas yet" message="Weak question types appear after scored attempts." />
  }

  return (
    <div className="feature-list">
      {dashboard.weakAreas.slice(0, 5).map((area) => (
        <RepeatedItemCard key={`${area.skill}-${area.questionType}`}>
          <Badge tone="warning">{humanizeToken(area.skill)}</Badge>
          <h3>{humanizeToken(area.questionType)}</h3>
          <DescriptionList
            items={[
              { label: 'Incorrect', value: area.incorrectCount },
              { label: 'Attempts', value: area.attemptCount },
              { label: 'Latest', value: formatDate(area.latestSeenAt) },
            ]}
          />
        </RepeatedItemCard>
      ))}
    </div>
  )
}

export function RecentAttempts({ attempts }: { attempts: RecentAttemptRDO[] }) {
  if (attempts.length === 0) {
    return <EmptyState title="No recent attempts" message="Your submitted and expired attempts will show up here." />
  }

  return (
    <div className="feature-list">
      {attempts.slice(0, 5).map((attempt) => (
        <RepeatedItemCard key={attempt.attemptId}>
          <Badge tone={attempt.attemptStatus === 'submitted' ? 'success' : 'warning'}>
            {humanizeToken(attempt.attemptStatus)}
          </Badge>
          <h3>{attempt.testTitle}</h3>
          <DescriptionList
            items={[
              { label: 'Band', value: formatNumber(attempt.estimatedBand) },
              { label: 'Time', value: formatSeconds(attempt.timeSpentSeconds) },
              { label: 'Submitted', value: formatDate(attempt.submittedAt ?? attempt.startedAt) },
            ]}
          />
          {attempt.resultId && (
            <Link className="feature-link" to={fillRoute(ROUTES.resultDetail, { id: attempt.resultId })}>
              View result
            </Link>
          )}
        </RepeatedItemCard>
      ))}
    </div>
  )
}

export function Recommendations({ recommendations }: { recommendations: ProgressRecommendationRDO[] }) {
  if (recommendations.length === 0) {
    return (
      <EmptyState
        title="No recommendations yet"
        message="Start a first Reading or Listening test to unlock more targeted suggestions."
        actions={
          <Link className="feature-link feature-link--primary" to={ROUTES.tests}>
            Find a test
          </Link>
        }
      />
    )
  }

  return (
    <div className="feature-list">
      {recommendations.slice(0, 4).map((recommendation) => (
        <RepeatedItemCard key={`${recommendation.sourceType}-${recommendation.testId ?? recommendation.title}`}>
          <div className="feature-card__badges">
            <Badge tone={recommendation.free ? 'success' : 'warning'}>
              {recommendation.free ? 'Free' : 'Paid'}
            </Badge>
            <Badge tone={recommendation.directlyAvailable ? 'success' : 'warning'}>
              {recommendation.directlyAvailable ? 'Ready' : 'Needs access'}
            </Badge>
          </div>
          <h3>{recommendation.title}</h3>
          <p className="feature-card__summary">{recommendation.reason}</p>
          <Link className="feature-link" to={recommendationPath(recommendation)}>
            {recommendation.directlyAvailable ? 'Start practice' : 'View options'}
          </Link>
        </RepeatedItemCard>
      ))}
    </div>
  )
}

export function DailyTask({ plans }: { plans: StudyPlanRDO[] }) {
  const plan = plans[0]
  const task = plan?.tasks.find((item) => !item.completed) ?? plan?.tasks[0]

  if (!plan || !task) {
    return (
      <EmptyState
        title="No active study task"
        message="Writing and speaking feedback can generate a plan once enough input is available."
        actions={
          <>
            <Link className="feature-link" to={ROUTES.writing}>
              Practice writing
            </Link>
            <Link className="feature-link" to={ROUTES.speaking}>
              Practice speaking
            </Link>
          </>
        }
      />
    )
  }

  return (
    <RepeatedItemCard>
      <Badge tone={task.completed ? 'success' : 'info'}>{humanizeToken(task.skill)}</Badge>
      <h3>{task.title}</h3>
      <p className="feature-card__summary">{task.description}</p>
      <DescriptionList
        items={[
          { label: 'Date', value: formatDate(task.taskDate) },
          { label: 'Time', value: `${task.estimatedMinutes} min` },
          { label: 'Target', value: formatNumber(plan.targetBand) },
          { label: 'Reason', value: task.rationale },
        ]}
      />
      {task.recommendedResource.id && (
        <Link
          className="feature-link"
          to={
            task.recommendedResource.type === 'listing'
              ? `${ROUTES.marketplace}/${task.recommendedResource.id}`
              : fillRoute(ROUTES.testDetail, { id: task.recommendedResource.id })
          }
        >
          Open resource
        </Link>
      )}
    </RepeatedItemCard>
  )
}

export function AiUsageCard({ usage }: { usage?: AiUsageRemainingRDO | null }) {
  if (!usage) {
    return (
      <EmptyState
        title="AI usage unavailable"
        message="AI writing and speaking usage will appear when the feature is enabled."
      />
    )
  }

  return (
    <RepeatedItemCard>
      <Badge tone={usage.hardLimitReached ? 'danger' : 'success'}>
        {usage.hardLimitReached ? 'Limit reached' : 'Available'}
      </Badge>
      <h3>{usage.freeRequestsRemaining} free requests left</h3>
      <DescriptionList
        items={[
          { label: 'Used', value: usage.usedRequests },
          { label: 'Free limit', value: usage.freeRequestsLimit },
          { label: 'Hard limit', value: usage.hardLimitRequests ?? 'None' },
          { label: 'Period', value: `${formatDate(usage.periodStart)} - ${formatDate(usage.periodEnd)}` },
        ]}
      />
    </RepeatedItemCard>
  )
}

