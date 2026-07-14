import { Link, useParams } from 'react-router-dom'

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
import { ROUTES, fillRoute } from '../../constants/routes'
import {
  useAccessibleListingItems,
  useGrantFreeAccess,
  useListingAccess,
  useMarketplaceListing,
} from '../../hooks/useMarketplace'
import { useSessionStore } from '../../store/sessionStore'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

export function MarketplaceDetailPage() {
  const listingId = Number(useParams().id)
  const session = useSessionStore()
  const isStudent =
    session.status === 'authenticated' && session.user.roles.includes('student') && Number.isFinite(listingId)
  const listingQuery = useMarketplaceListing(Number.isFinite(listingId) ? listingId : undefined)
  const accessQuery = useListingAccess(listingId, isStudent)
  const hasAccess = accessQuery.data?.accessible || listingQuery.data?.free
  const itemsQuery = useAccessibleListingItems(listingId, isStudent && Boolean(hasAccess))
  const freeAccess = useGrantFreeAccess(listingId)

  if (!Number.isFinite(listingId)) {
    return (
      <PageContainer>
        <EmptyState
          title="Listing not found"
          message="The marketplace listing URL is not valid."
          actions={
            <Link className="feature-link" to={ROUTES.marketplace}>
              Back to marketplace
            </Link>
          }
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Marketplace"
        title={listingQuery.data?.title ?? 'Listing detail'}
        actions={
          <Link className="feature-link" to={ROUTES.marketplace}>
            Back to marketplace
          </Link>
        }
      />

      {listingQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading listing" />
            <Skeleton lines={5} />
          </div>
        </Panel>
      )}

      {listingQuery.isError && (
        <Panel>
          <div className="feature-panel-state">
            <Alert title="Listing could not be loaded" variant="danger">
              {listingQuery.error instanceof Error
                ? listingQuery.error.message
                : 'The listing may be unavailable.'}
            </Alert>
          </div>
        </Panel>
      )}

      {listingQuery.data && (
        <div className="feature-detail-grid">
          <Panel title={listingQuery.data.title} description={listingQuery.data.description ?? undefined}>
            <div className="feature-panel-body">
              <div className="feature-card__badges">
                <Badge tone={listingQuery.data.free ? 'success' : 'warning'}>
                  {listingQuery.data.free
                    ? 'Free'
                    : formatCurrency(listingQuery.data.priceAmount, listingQuery.data.currency, 'Paid')}
                </Badge>
                <Badge tone="info">{humanizeToken(listingQuery.data.listingType)}</Badge>
                {hasAccess && <Badge tone="success">Access available</Badge>}
              </div>
              <DescriptionList
                items={[
                  { label: 'Teacher', value: listingQuery.data.teacher.displayName },
                  { label: 'Language', value: listingQuery.data.language.toUpperCase() },
                  { label: 'Published', value: formatDate(listingQuery.data.createdAt) },
                  { label: 'Updated', value: formatDate(listingQuery.data.updatedAt) },
                ]}
              />
              <div className="feature-card__tags">
                {listingQuery.data.categories.map((category) => (
                  <span key={category.id}>{category.name}</span>
                ))}
                {listingQuery.data.tags.map((tag) => (
                  <span key={tag.id}>{tag.name}</span>
                ))}
              </div>
            </div>
          </Panel>

          <Panel title="Access" description="Free access is granted by the backend. Paid checkout lands later.">
            <div className="feature-panel-body feature-stack">
              {session.status !== 'authenticated' && (
                <Alert title="Sign in required">
                  Log in as a student to start tests, claim free listings, or open protected items.
                </Alert>
              )}

              {session.status === 'authenticated' && !session.user.roles.includes('student') && (
                <Alert title="Student access required" variant="warning">
                  Marketplace access is available to student accounts.
                </Alert>
              )}

              {listingQuery.data.listingType === 'speaking_question_pack' && (
                <Alert title="Live speaking stays separate" variant="warning">
                  Scheduled speaking sessions are not booked from marketplace listings yet.
                </Alert>
              )}

              {listingQuery.data.free && isStudent && !accessQuery.data?.accessible && (
                <Button
                  isLoading={freeAccess.isPending}
                  loadingLabel="Granting access"
                  onClick={() => freeAccess.mutate()}
                >
                  Get free access
                </Button>
              )}

              {!listingQuery.data.free && !hasAccess && (
                <Link className="feature-link feature-link--primary" to={ROUTES.pricing}>
                  Purchase required
                </Link>
              )}

              {listingQuery.data.testId && hasAccess && (
                <Link
                  className="feature-link feature-link--primary"
                  to={fillRoute(ROUTES.testDetail, { id: listingQuery.data.testId })}
                >
                  Open linked test
                </Link>
              )}

              {freeAccess.isError && (
                <Alert title="Access could not be granted" variant="danger">
                  {freeAccess.error instanceof Error
                    ? freeAccess.error.message
                    : 'Try again or refresh the listing.'}
                </Alert>
              )}
            </div>
          </Panel>

          <Panel title="Included items" description="Protected item identifiers appear only when access allows it.">
            <div className="feature-panel-body">
              {itemsQuery.isLoading && <Loader label="Checking included items" />}

              {!itemsQuery.isLoading &&
                (itemsQuery.data?.items.length || listingQuery.data.items.length) === 0 && (
                  <EmptyState
                    title="No visible items yet"
                    message="Paid listings may hide item details until access is granted."
                  />
                )}

              <div className="feature-card-grid feature-card-grid--compact">
                {(itemsQuery.data?.items ?? listingQuery.data.items).map((item) => (
                  <RepeatedItemCard key={item.id}>
                    <Badge tone="info">Item {item.itemOrder}</Badge>
                    <h3>{item.title ?? 'Marketplace item'}</h3>
                    {item.description && <p className="feature-card__summary">{item.description}</p>}
                    {item.testId && (
                      <Link
                        className="feature-link"
                        to={fillRoute(ROUTES.testDetail, { id: item.testId })}
                      >
                        Open test
                      </Link>
                    )}
                  </RepeatedItemCard>
                ))}
              </div>
            </div>
          </Panel>
        </div>
      )}
    </PageContainer>
  )
}

