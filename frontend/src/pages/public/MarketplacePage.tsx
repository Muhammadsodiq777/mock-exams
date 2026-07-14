import { Link } from 'react-router-dom'

import { MarketplaceCard } from '../../components/discovery/DiscoveryCards'
import { MarketplaceFilters } from '../../components/discovery/DiscoveryFilters'
import {
  Alert,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Pagination,
  Panel,
  Skeleton,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useMarketplaceListings } from '../../hooks/useMarketplace'
import { useMarketplaceFilters } from '../../hooks/useUrlFilters'

export function MarketplacePage() {
  const { filters, updateFilters } = useMarketplaceFilters()
  const listingsQuery = useMarketplaceListings(filters)
  const pageMeta = listingsQuery.data?.meta.page
  const listings = listingsQuery.data?.items ?? []

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Marketplace"
        title="Teacher content"
        actions={
          <Link className="feature-link" to={ROUTES.tests}>
            Browse tests
          </Link>
        }
      />

      <Panel
        title="Discover practice material"
        description="Search approved teacher listings, packs, courses, and free resources."
      >
        <MarketplaceFilters filters={filters} onChange={updateFilters} />

        {listingsQuery.isLoading && (
          <div className="feature-panel-state">
            <Loader label="Loading marketplace" />
            <Skeleton lines={4} />
          </div>
        )}

        {listingsQuery.isError && (
          <div className="feature-panel-state">
            <Alert title="Marketplace could not be loaded" variant="danger">
              {listingsQuery.error instanceof Error
                ? listingsQuery.error.message
                : 'Refresh or adjust the filters.'}
            </Alert>
          </div>
        )}

        {!listingsQuery.isLoading && !listingsQuery.isError && listings.length === 0 && (
          <EmptyState
            className="feature-panel-state"
            title="No listings match these filters"
            message="Try another content type, price range, teacher, tag, category, or search term."
            actions={
              <button
                className="feature-link"
                onClick={() =>
                  updateFilters({
                    category: undefined,
                    categories: undefined,
                    language: undefined,
                    maxPrice: undefined,
                    minPrice: undefined,
                    page: 0,
                    price: undefined,
                    q: undefined,
                    tag: undefined,
                    tags: undefined,
                    teacherId: undefined,
                    type: undefined,
                  })
                }
                type="button"
              >
                Clear filters
              </button>
            }
          />
        )}

        {!listingsQuery.isLoading && !listingsQuery.isError && listings.length > 0 && (
          <>
            <div className="feature-card-grid">
              {listings.map((listing) => (
                <MarketplaceCard key={listing.id} listing={listing} />
              ))}
            </div>
            {pageMeta && (
              <Pagination
                canGoNext={!pageMeta.last}
                canGoPrevious={!pageMeta.first}
                isLoading={listingsQuery.isFetching}
                onNext={() => updateFilters({ page: pageMeta.number + 1 })}
                onPrevious={() => updateFilters({ page: Math.max(0, pageMeta.number - 1) })}
                page={pageMeta.number + 1}
                pageSize={pageMeta.size}
                totalItems={pageMeta.totalElements}
                totalPages={pageMeta.totalPages}
              />
            )}
          </>
        )}
      </Panel>
    </PageContainer>
  )
}

