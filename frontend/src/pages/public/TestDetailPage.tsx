import { Link, useParams } from 'react-router-dom'

import { ListeningTestPlayer } from '../../components/player/ListeningTestPlayer'
import { ReadingTestPlayer } from '../../components/player/ReadingTestPlayer'
import {
  Alert,
  Badge,
  DescriptionList,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  Skeleton,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useCatalogTest } from '../../hooks/useCatalog'
import { formatCurrency, formatMinutes, formatNumber, humanizeToken } from '../../utils/formatters'

export function TestDetailPage() {
  const testId = Number(useParams().id)
  const testQuery = useCatalogTest(Number.isFinite(testId) ? testId : undefined)
  const test = testQuery.data
  const playerMounted = test?.type === 'reading' || test?.type === 'listening'

  if (!Number.isFinite(testId)) {
    return (
      <PageContainer>
        <EmptyState
          title="Test not found"
          message="The test URL is not valid."
          actions={
            <Link className="feature-link" to={ROUTES.tests}>
              Back to tests
            </Link>
          }
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer size={test?.type === 'reading' || test?.type === 'listening' ? 'wide' : 'default'}>
      <PageHeader
        eyebrow="Test detail"
        title={test?.title ?? 'Loading test'}
        actions={
          playerMounted ? undefined : (
            <Link className="feature-link" to={ROUTES.tests}>
              Back to tests
            </Link>
          )
        }
      />

      {testQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading test detail" />
            <Skeleton lines={5} />
          </div>
        </Panel>
      )}

      {testQuery.isError && (
        <Panel>
          <div className="feature-panel-state">
            <Alert title="Test could not be loaded" variant="danger">
              {testQuery.error instanceof Error
                ? testQuery.error.message
                : 'This test may be unpublished or unavailable.'}
            </Alert>
          </div>
        </Panel>
      )}

      {test && (
        <>
          <Panel title="Overview" description={test.description ?? undefined}>
            <div className="feature-panel-body">
              <div className="feature-card__badges">
                <Badge tone="info">{humanizeToken(test.type)}</Badge>
                <Badge tone={test.free ? 'success' : 'warning'}>
                  {test.free ? 'Free' : formatCurrency(test.priceAmount, test.currency, 'Paid')}
                </Badge>
                <Badge tone="info">{test.language.toUpperCase()}</Badge>
              </div>
              <DescriptionList
                items={[
                  { label: 'Duration', value: formatMinutes(test.durationMinutes) },
                  {
                    label: 'Band range',
                    value:
                      test.estimatedBandMin || test.estimatedBandMax
                        ? `${formatNumber(test.estimatedBandMin, '?')}-${formatNumber(test.estimatedBandMax, '?')}`
                        : 'Adaptive',
                  },
                  { label: 'Difficulty', value: humanizeToken(test.difficulty, 'Any level') },
                  { label: 'Teacher', value: test.teacher?.displayName ?? 'Official' },
                ]}
              />
            </div>
          </Panel>

          {test.type === 'reading' ? (
            <ReadingTestPlayer test={test} />
          ) : test.type === 'listening' ? (
            <ListeningTestPlayer test={test} />
          ) : (
            <Panel>
              <EmptyState
                className="feature-panel-state"
                title={`${humanizeToken(test.type)} player is not ready yet`}
                message="This route now supports Reading and Listening players. Writing, Speaking, and full mock players will mount in later plans."
                actions={
                  <Link className="feature-link feature-link--primary" to={`${ROUTES.tests}?type=reading`}>
                    Find Reading tests
                  </Link>
                }
              />
            </Panel>
          )}
        </>
      )}
    </PageContainer>
  )
}
