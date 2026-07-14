import { Link } from 'react-router-dom'

import {
  AiUsageCard,
  BandSummary,
  DailyTask,
  ProgressChart,
  RecentAttempts,
  Recommendations,
  SkillOverview,
  WeakAreas,
} from '../../components/dashboard/StudentDashboardPanels'
import {
  Alert,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  Skeleton,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useActiveStudyPlans, useAiUsage, useStudentProgress } from '../../hooks/useStudentProgress'
import { featureFlags } from '../../services/api'

export function DashboardPage() {
  const progressQuery = useStudentProgress()
  const studyPlansQuery = useActiveStudyPlans()
  const aiUsageQuery = useAiUsage(featureFlags.ai)
  const dashboard = progressQuery.data?.dashboard
  const plans = studyPlansQuery.data?.items ?? []
  const hasProgressData = dashboard && dashboard.state !== 'empty'

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Student"
        title="Dashboard"
        actions={
          <>
            <Link className="feature-link feature-link--primary" to={ROUTES.tests}>
              Find a test
            </Link>
            <Link className="feature-link" to={ROUTES.marketplace}>
              Browse marketplace
            </Link>
          </>
        }
      />

      {progressQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading progress" />
            <Skeleton lines={5} />
          </div>
        </Panel>
      )}

      {progressQuery.isError && (
        <Panel>
          <div className="feature-panel-state">
            <Alert title="Progress could not be loaded" variant="danger">
              {progressQuery.error instanceof Error
                ? progressQuery.error.message
                : 'Refresh the dashboard and try again.'}
            </Alert>
          </div>
        </Panel>
      )}

      {dashboard && dashboard.state === 'empty' && (
        <Panel>
          <EmptyState
            className="feature-panel-state"
            eyebrow="New student"
            title="Start your first practice loop"
            message="Take a Reading or Listening test first. Writing and Speaking data will be labelled as missing until you add those skills."
            actions={
              <>
                <Link className="feature-link feature-link--primary" to={`${ROUTES.tests}?type=reading&price=free`}>
                  First Reading test
                </Link>
                <Link className="feature-link" to={`${ROUTES.tests}?type=listening&price=free`}>
                  First Listening test
                </Link>
                <Link className="feature-link" to={ROUTES.speaking}>
                  AI speaking practice
                </Link>
              </>
            }
          />
        </Panel>
      )}

      {hasProgressData && (
        <div className="dashboard-grid">
          <section className="dashboard-grid__summary">
            <BandSummary dashboard={dashboard} />
          </section>

          <Panel title="Skill bands" description="Missing skills are prompts to practice, not zero scores.">
            <div className="feature-panel-body">
              <SkillOverview dashboard={dashboard} />
            </div>
          </Panel>

          <Panel title="Progress" description="Recent scored objective attempts by IELTS skill.">
            <div className="feature-panel-body">
              <ProgressChart dashboard={dashboard} />
            </div>
          </Panel>

          <Panel title="Daily task" description="Current active study plan task, when available.">
            <div className="feature-panel-body">
              {studyPlansQuery.isLoading ? <Loader label="Loading study plan" /> : <DailyTask plans={plans} />}
            </div>
          </Panel>

          <Panel title="Weak areas" description="Question types that deserve attention.">
            <div className="feature-panel-body">
              <WeakAreas dashboard={dashboard} />
            </div>
          </Panel>

          <Panel title="Recent attempts" description="Submitted and expired attempts from your progress window.">
            <div className="feature-panel-body">
              <RecentAttempts attempts={dashboard.recentAttempts} />
            </div>
          </Panel>

          <Panel title="Recommendations" description="Startable tests and content suggested by progress rules.">
            <div className="feature-panel-body">
              <Recommendations recommendations={dashboard.recommendations} />
            </div>
          </Panel>

          {featureFlags.ai && (
            <Panel title="AI usage" description="Writing and speaking request allowance.">
              <div className="feature-panel-body">
                {aiUsageQuery.isLoading ? (
                  <Loader label="Loading AI usage" />
                ) : (
                  <AiUsageCard usage={dashboard.aiUsage ?? aiUsageQuery.data} />
                )}
              </div>
            </Panel>
          )}
        </div>
      )}
    </PageContainer>
  )
}
