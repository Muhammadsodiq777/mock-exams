import { Link, useParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  DescriptionList,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  RichContent,
  Skeleton,
} from '../../components/ui'
import { ROUTES, fillRoute } from '../../constants/routes'
import { useResultDetail } from '../../hooks/useResultDetail'
import { useStudentProgress } from '../../hooks/useStudentProgress'
import type { ResultAnswerReviewRDO, ResultDetailRDO } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatDate, formatNumber, formatSeconds, humanizeToken } from '../../utils/formatters'

const answerStatus = (answer: ResultAnswerReviewRDO) => {
  if (answer.unanswered) {
    return { label: 'Unanswered', tone: 'warning' as const }
  }

  return answer.correct
    ? { label: 'Correct', tone: 'success' as const }
    : { label: 'Incorrect', tone: 'danger' as const }
}

const groupedAnswers = (result: ResultDetailRDO) =>
  [...result.sections]
    .sort((a, b) => a.sectionOrder - b.sectionOrder)
    .map((section) => ({
      section,
      answers: result.answers
        .filter((answer) => answer.sectionId === section.id)
        .sort((a, b) => a.questionOrder - b.questionOrder),
    }))

function ResultProgressChart({ result }: { result: ResultDetailRDO }) {
  const progressQuery = useStudentProgress()
  const skill = result.testType
  const points = progressQuery.data?.dashboard.skillHistories[skill] ?? []

  if (progressQuery.isLoading) {
    return <Loader label="Loading progress history" />
  }

  if (progressQuery.isError) {
    return (
      <Alert title="Progress history unavailable" variant="warning">
        {progressQuery.error instanceof Error ? progressQuery.error.message : 'Refresh later to compare progress.'}
      </Alert>
    )
  }

  if (points.length === 0) {
    return <EmptyState title="No history yet" message="This score will appear in your progress once indexed." />
  }

  return (
    <div className="dashboard-chart" aria-label={`${humanizeToken(skill)} progress history`}>
      {points.slice(-8).map((point) => {
        const band = Number(point.estimatedBand ?? 0)
        const height = Number.isFinite(band) ? Math.max(8, (band / 9) * 100) : 8

        return (
          <Link
            className="dashboard-chart__bar result-chart-link"
            key={point.resultId}
            to={fillRoute(ROUTES.resultDetail, { id: point.resultId })}
          >
            <span style={{ height: `${height}%` }} />
            <p>{formatDate(point.scoredAt)}</p>
            <strong>{formatNumber(point.estimatedBand, '-')}</strong>
          </Link>
        )
      })}
    </div>
  )
}

function AnswerReview({ answer, result }: { answer: ResultAnswerReviewRDO; result: ResultDetailRDO }) {
  const status = answerStatus(answer)
  const weakArea = result.weakAreas.find(
    (area) => area.sectionId === answer.sectionId && area.questionType === answer.questionType,
  )

  return (
    <RepeatedItemCard className={`result-answer result-answer--${status.label.toLowerCase()}`}>
      <div className="feature-card__badges">
        <Badge tone={status.tone}>{status.label}</Badge>
        <Badge tone="info">Question {answer.questionOrder}</Badge>
        <Badge tone="info">{humanizeToken(answer.questionType)}</Badge>
        {weakArea && <Badge tone="warning">Weak area</Badge>}
      </div>
      {answer.questionRenderedHtml ? (
        <RichContent trustedHtml={answer.questionRenderedHtml} />
      ) : (
        <RichContent blocks={[{ text: answer.questionText, type: 'paragraph' }]} />
      )}
      <DescriptionList
        items={[
          {
            label: 'Your answer',
            value:
              answer.unanswered || (!answer.studentAnswer && !answer.selectedOptionLabel)
                ? 'No answer'
                : answer.selectedOptionLabel ?? answer.studentAnswer,
          },
          {
            label: 'Correct answer',
            value: answer.correctAnswers.length ? answer.correctAnswers.join(', ') : 'Not provided',
          },
          {
            label: 'Score',
            value: `${formatNumber(answer.scoreAwarded, '0')} / ${formatNumber(answer.maxScore, '-')}`,
          },
          { label: 'Section', value: answer.sectionTitle },
        ]}
      />
      {(answer.explanationContent?.renderedHtml || answer.explanationContent?.contentText || answer.explanation) && (
        <div className="result-explanation">
          <h3>Explanation</h3>
          {answer.explanationContent?.renderedHtml ? (
            <RichContent trustedHtml={answer.explanationContent.renderedHtml} />
          ) : (
            <RichContent
              blocks={[
                {
                  text: answer.explanationContent?.contentText ?? answer.explanation ?? '',
                  type: 'paragraph',
                },
              ]}
            />
          )}
        </div>
      )}
    </RepeatedItemCard>
  )
}

export function ResultDetailPage() {
  const resultId = Number(useParams().id)
  const resultQuery = useResultDetail(Number.isFinite(resultId) ? resultId : undefined)
  const result = resultQuery.data
  const error = resultQuery.isError ? normalizeApiError(resultQuery.error) : null

  if (!Number.isFinite(resultId)) {
    return (
      <PageContainer>
        <EmptyState
          title="Result not found"
          message="The result URL is not valid."
          actions={
            <Link className="feature-link" to={ROUTES.dashboard}>
              Back to dashboard
            </Link>
          }
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Result review"
        title={result?.testTitle ?? 'Loading result'}
        actions={
          <>
            <Link className="feature-link" to={ROUTES.dashboard}>
              Dashboard
            </Link>
            <Link className="feature-link" to={ROUTES.tests}>
              Find tests
            </Link>
          </>
        }
      />

      {resultQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading result" />
            <Skeleton lines={6} />
          </div>
        </Panel>
      )}

      {error && (
        <Panel>
          <div className="feature-panel-state">
            <Alert
              title={
                error.code === 'RESULT_NOT_READY' || error.code === 'ATTEMPT_NOT_SUBMITTED'
                  ? 'Result is not ready'
                  : error.status === 401 || error.status === 403
                    ? 'Result is private'
                    : 'Result could not be loaded'
              }
              variant={error.code === 'RESULT_NOT_READY' ? 'warning' : 'danger'}
            >
              {error.status === 401 || error.status === 403
                ? 'Sign in with the student account that owns this result.'
                : error.message}
            </Alert>
            <div className="feature-card__actions">
              <ButtonLikeRefresh isFetching={resultQuery.isFetching} onClick={() => void resultQuery.refetch()} />
              <Link className="feature-link" to={ROUTES.dashboard}>
                Back to dashboard
              </Link>
            </div>
          </div>
        </Panel>
      )}

      {result && (
        <div className="feature-stack">
          <div className="result-summary-grid">
            <RepeatedItemCard>
              <Badge tone="info">Raw score</Badge>
              <p className="dashboard-metric">{formatNumber(result.rawScore, '0')}</p>
            </RepeatedItemCard>
            <RepeatedItemCard>
              <Badge tone="info">Max score</Badge>
              <p className="dashboard-metric">{formatNumber(result.maxScore, '-')}</p>
            </RepeatedItemCard>
            <RepeatedItemCard>
              <Badge tone="success">Estimated band</Badge>
              <p className="dashboard-metric">{formatNumber(result.estimatedBand)}</p>
            </RepeatedItemCard>
            <RepeatedItemCard>
              <Badge tone="info">Time spent</Badge>
              <p className="dashboard-metric">{formatSeconds(result.timeSpentSeconds)}</p>
            </RepeatedItemCard>
            <RepeatedItemCard>
              <Badge tone="info">Completed</Badge>
              <p className="dashboard-metric">{formatDate(result.submittedAt ?? result.startedAt)}</p>
            </RepeatedItemCard>
          </div>

          <Panel title="Progress history" description={`${humanizeToken(result.testType)} scores from your dashboard window.`}>
            <div className="feature-panel-body">
              <ResultProgressChart result={result} />
            </div>
          </Panel>

          {result.weakAreas.length > 0 && (
            <Panel title="Weak areas" description="Tags are generated by the backend result detail.">
              <div className="feature-card-grid feature-card-grid--compact">
                {result.weakAreas.map((area) => (
                  <RepeatedItemCard key={`${area.sectionId}-${area.questionType}`}>
                    <Badge tone="warning">{area.sectionTitle}</Badge>
                    <h3>{humanizeToken(area.questionType)}</h3>
                    <DescriptionList
                      items={[
                        { label: 'Incorrect', value: area.incorrectCount },
                        { label: 'Total', value: area.totalCount },
                      ]}
                    />
                  </RepeatedItemCard>
                ))}
              </div>
            </Panel>
          )}

          <Panel title="Answer review" description="Correct answers and explanations come from the submitted result only.">
            <div className="feature-panel-body">
              {groupedAnswers(result).map(({ answers, section }) => (
                <section className="result-section" key={section.id}>
                  <div className="result-section__header">
                    <h3>{section.title}</h3>
                    <div className="feature-card__badges">
                      <Badge tone="success">{section.correctCount} correct</Badge>
                      <Badge tone="warning">{section.incorrectCount} missed</Badge>
                      <Badge tone="info">
                        {formatNumber(section.rawScore, '0')} / {formatNumber(section.maxScore, '-')}
                      </Badge>
                    </div>
                  </div>
                  <div className="feature-list">
                    {answers.map((answer) => (
                      <AnswerReview answer={answer} key={answer.questionId} result={result} />
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </Panel>

          <Panel title="Next practice" description="Use this result to start another loop.">
            <div className="result-cta-grid">
              <Link className="feature-link feature-link--primary" to={ROUTES.dashboard}>
                Dashboard
              </Link>
              <Link className="feature-link" to={`${ROUTES.tests}?type=${result.testType}`}>
                Similar tests
              </Link>
              <Link className="feature-link" to={ROUTES.marketplace}>
                Marketplace
              </Link>
              <Link className="feature-link" to={ROUTES.writing}>
                Writing practice
              </Link>
              <Link className="feature-link" to={ROUTES.speaking}>
                Speaking practice
              </Link>
            </div>
          </Panel>
        </div>
      )}
    </PageContainer>
  )
}

function ButtonLikeRefresh({ isFetching, onClick }: { isFetching: boolean; onClick: () => void }) {
  return (
    <button className="feature-link" disabled={isFetching} onClick={onClick} type="button">
      {isFetching ? 'Refreshing' : 'Refresh'}
    </button>
  )
}
