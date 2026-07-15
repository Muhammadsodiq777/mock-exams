import { Link } from 'react-router-dom'

import heroImage from '../../assets/hero.png'
import { TestCard } from '../../components/discovery/DiscoveryCards'
import { TestCatalogFilters } from '../../components/discovery/DiscoveryFilters'
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
import { useCatalogTests } from '../../hooks/useCatalog'
import { useTestCatalogFilters } from '../../hooks/useUrlFilters'

export function TestsPage() {
  const { filters, updateFilters } = useTestCatalogFilters()
  const testsQuery = useCatalogTests(filters)
  const pageMeta = testsQuery.data?.meta.page
  const tests = testsQuery.data?.items ?? []

  return (
    <PageContainer size="wide">
      <section className="exam-hero" aria-labelledby="exam-hero-title">
        <div className="exam-hero__copy">
          <p className="ui-eyebrow">IELTS mock exam platform</p>
          <h1 id="exam-hero-title">Practice under exam conditions</h1>
          <p>
            Browse timed Reading and Listening tests, teacher-created packs, and AI practice flows
            in one focused workspace.
          </p>
          <div className="exam-hero__stats" aria-label="Platform highlights">
            <span>Timed players</span>
            <span>Band-focused filters</span>
            <span>Teacher marketplace</span>
          </div>
        </div>
        <div className="exam-hero__visual" aria-hidden="true">
          <img src={heroImage} alt="" />
          <div>
            <strong>60:00</strong>
            <span>Reading mock ready</span>
          </div>
        </div>
      </section>

      <PageHeader
        eyebrow="Mock Exams"
        title="IELTS practice tests"
        actions={
          <Link className="feature-link" to={ROUTES.marketplace}>
            Browse marketplace
          </Link>
        }
      />

      <Panel
        title="Find a test"
        description="Filter official and approved teacher tests by IELTS skill, band, price, tags, and language."
      >
        <TestCatalogFilters filters={filters} onChange={updateFilters} />

        {testsQuery.isLoading && (
          <div className="feature-panel-state">
            <Loader label="Loading tests" />
            <Skeleton lines={4} />
          </div>
        )}

        {testsQuery.isError && (
          <div className="feature-panel-state">
            <Alert title="Tests could not be loaded" variant="danger">
              {testsQuery.error instanceof Error
                ? testsQuery.error.message
                : 'Refresh or adjust the filters.'}
            </Alert>
          </div>
        )}

        {!testsQuery.isLoading && !testsQuery.isError && tests.length === 0 && (
          <EmptyState
            className="feature-panel-state"
            title="No tests match these filters"
            message="Try a broader IELTS type, band range, price, or category."
            actions={
              <button
                className="feature-link"
                onClick={() =>
                  updateFilters({
                    bandMax: undefined,
                    bandMin: undefined,
                    category: undefined,
                    categories: undefined,
                    difficulty: undefined,
                    language: undefined,
                    page: 0,
                    price: undefined,
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

        {!testsQuery.isLoading && !testsQuery.isError && tests.length > 0 && (
          <>
            <div className="feature-card-grid">
              {tests.map((test) => (
                <TestCard key={test.id} test={test} />
              ))}
            </div>
            {pageMeta && (
              <Pagination
                canGoNext={!pageMeta.last}
                canGoPrevious={!pageMeta.first}
                isLoading={testsQuery.isFetching}
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
