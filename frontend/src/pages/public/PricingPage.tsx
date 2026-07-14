import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  Skeleton,
} from '../../components/ui'
import { ROUTES, fillRoute } from '../../constants/routes'
import { isBrowserCheckoutRedirect, useCreateMarketplaceCheckout } from '../../hooks/usePaymentCheckout'
import { usePricingListings, useSubscriptionPlans } from '../../hooks/usePricing'
import { useStudentPurchases } from '../../hooks/useStudentProfile'
import { featureFlags } from '../../services/api'
import { useSessionStore } from '../../store/sessionStore'
import type { MarketplaceListingSummaryRDO, PaymentRDO, PurchaseRDO } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatCurrency, humanizeToken } from '../../utils/formatters'

type CheckoutNotice =
  | {
      kind: 'redirecting'
      payment: PaymentRDO
      title: string
    }
  | {
      kind: 'mock' | 'no-checkout' | 'success'
      payment: PaymentRDO
      title: string
      message: string
    }

const checkoutErrorTitle = (code: string) => {
  if (code === 'PAYMENT_PROVIDER_DISABLED') {
    return 'Payment provider disabled'
  }

  if (code === 'PAYMENT_TARGET_UNSUPPORTED') {
    return 'Checkout target unsupported'
  }

  if (code === 'PAYMENT_REQUIRED') {
    return 'Payment required'
  }

  if (code === 'ALREADY_PURCHASED') {
    return 'Access already active'
  }

  if (code === 'VALIDATION_FAILED') {
    return 'Checkout details need attention'
  }

  return 'Checkout could not be started'
}

function PricingCard({
  disabledReason,
  isCheckoutPending,
  listing,
  onCheckout,
  purchase,
}: {
  disabledReason: string | null
  isCheckoutPending: boolean
  listing: MarketplaceListingSummaryRDO
  onCheckout: (listing: MarketplaceListingSummaryRDO) => void
  purchase?: PurchaseRDO
}) {
  const hasAccess = Boolean(listing.accessStatus?.accessible || purchase?.accessStatus.accessible)

  return (
    <RepeatedItemCard className="pricing-card">
      <div className="feature-card__badges">
        <Badge tone={listing.listingType === 'subscription_plan' ? 'success' : 'info'}>
          {humanizeToken(listing.listingType)}
        </Badge>
        <Badge tone={hasAccess ? 'success' : 'warning'}>
          {hasAccess ? 'Active access' : formatCurrency(listing.priceAmount, listing.currency)}
        </Badge>
      </div>
      <h3>{listing.title}</h3>
      {listing.description && <p className="feature-card__summary">{listing.description}</p>}
      <p className="pricing-card__price">{formatCurrency(listing.priceAmount, listing.currency)}</p>
      <p className="feature-card__note">
        {listing.itemCount > 0
          ? `${listing.itemCount} included item${listing.itemCount === 1 ? '' : 's'}`
          : 'Included items are configured by the teacher.'}
      </p>
      <div className="feature-card__actions">
        <Link className="feature-link" to={fillRoute(ROUTES.marketplaceListing, { id: listing.id })}>
          Details
        </Link>
        {hasAccess ? (
          <Button disabled variant="secondary">
            Active
          </Button>
        ) : disabledReason ? (
          <Button disabled variant="secondary">
            {disabledReason}
          </Button>
        ) : (
          <Button
            isLoading={isCheckoutPending}
            loadingLabel="Starting checkout"
            onClick={() => onCheckout(listing)}
          >
            Start checkout
          </Button>
        )}
      </div>
    </RepeatedItemCard>
  )
}

export function PricingPage() {
  const session = useSessionStore()
  const subscriptionPlansQuery = useSubscriptionPlans()
  const pricingListingsQuery = usePricingListings()
  const canLoadStudentAccess =
    session.status === 'authenticated' && session.user.roles.includes('student')
  const purchasesQuery = useStudentPurchases(canLoadStudentAccess)
  const checkout = useCreateMarketplaceCheckout()
  const [activeListingId, setActiveListingId] = useState<number | null>(null)
  const [checkoutNotice, setCheckoutNotice] = useState<CheckoutNotice | null>(null)
  const checkoutError = checkout.isError ? normalizeApiError(checkout.error) : null

  const subscriptionPlans = subscriptionPlansQuery.data?.items ?? []
  const paidListings = useMemo(
    () =>
      (pricingListingsQuery.data?.items ?? []).filter(
        (listing) => listing.listingType !== 'subscription_plan',
      ),
    [pricingListingsQuery.data?.items],
  )
  const accessiblePurchasesByListingId = useMemo(() => {
    const purchases = purchasesQuery.data?.items ?? []
    return new Map(
      purchases
        .filter((purchase) => purchase.accessStatus.accessible)
        .map((purchase) => [purchase.listingId, purchase] as const),
    )
  }, [purchasesQuery.data?.items])
  const isLoading = subscriptionPlansQuery.isLoading || pricingListingsQuery.isLoading
  const isError = subscriptionPlansQuery.isError || pricingListingsQuery.isError
  const hasResults = subscriptionPlans.length > 0 || paidListings.length > 0

  const handleCheckoutResult = (payment: PaymentRDO) => {
    const checkoutUrl = payment.providerCheckoutUrl

    if (checkoutUrl && isBrowserCheckoutRedirect(checkoutUrl)) {
      setCheckoutNotice({
        kind: 'redirecting',
        payment,
        title: 'Redirecting to checkout',
      })
      window.location.assign(checkoutUrl)
      return
    }

    if (checkoutUrl) {
      setCheckoutNotice({
        kind: 'mock',
        payment,
        title: 'Mock checkout created',
        message: 'The backend returned a provider checkout URL for the mock payment provider.',
      })
      return
    }

    if (payment.status === 'paid') {
      setCheckoutNotice({
        kind: 'success',
        payment,
        title: 'Payment completed',
        message: 'The backend completed this payment without a provider redirect.',
      })
      return
    }

    setCheckoutNotice({
      kind: 'no-checkout',
      payment,
      title: 'Checkout pending',
      message: 'The payment was created, but the provider did not return a checkout URL.',
    })
  }

  const disabledReason = (() => {
    if (session.status === 'loading') {
      return 'Checking session'
    }

    if (session.status === 'anonymous') {
      return null
    }

    if (!session.user.roles.includes('student')) {
      return 'Student only'
    }

    if (!featureFlags.payments) {
      return 'Checkout disabled'
    }

    return null
  })()

  const handleCheckout = (listing: MarketplaceListingSummaryRDO) => {
    if (session.status === 'anonymous') {
      return
    }

    setActiveListingId(listing.id)
    setCheckoutNotice(null)
    checkout.mutate(
      { listingId: listing.id },
      {
        onSuccess: handleCheckoutResult,
      },
    )
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Pricing"
        title="Plans and paid practice"
        actions={
          <>
            <Link className="feature-link" to={ROUTES.marketplace}>
              Marketplace
            </Link>
            {session.status === 'authenticated' ? (
              <Link className="feature-link feature-link--primary" to={ROUTES.profile}>
                Profile
              </Link>
            ) : (
              <Link className="feature-link feature-link--primary" to={ROUTES.login}>
                Sign in
              </Link>
            )}
          </>
        }
      />

      {!featureFlags.payments && (
        <Alert title="Checkout disabled" variant="warning">
          Paid listings are shown from backend pricing data, but payment checkout is hidden until the
          payment feature flag and provider are enabled.
        </Alert>
      )}

      {checkoutNotice && (
        <Alert
          title={checkoutNotice.title}
          variant={checkoutNotice.kind === 'success' ? 'success' : 'info'}
        >
          <p>{checkoutNotice.kind === 'redirecting' ? 'Opening the provider checkout page.' : checkoutNotice.message}</p>
          {checkoutNotice.payment.providerCheckoutUrl && !isBrowserCheckoutRedirect(checkoutNotice.payment.providerCheckoutUrl) && (
            <a className="feature-link" href={checkoutNotice.payment.providerCheckoutUrl}>
              Open provider checkout
            </a>
          )}
        </Alert>
      )}

      {checkoutError && (
        <Alert title={checkoutErrorTitle(checkoutError.code)} variant="danger">
          <p>{checkoutError.message}</p>
          {checkoutError.fieldErrors.length > 0 && (
            <ul className="pricing-error-list">
              {checkoutError.fieldErrors.map((fieldError) => (
                <li key={`${fieldError.field}-${fieldError.code}`}>{fieldError.message}</li>
              ))}
            </ul>
          )}
        </Alert>
      )}

      {isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading pricing" />
            <Skeleton lines={5} />
          </div>
        </Panel>
      )}

      {isError && (
        <Panel>
          <div className="feature-panel-state">
            <Alert title="Pricing could not be loaded" variant="danger">
              Backend pricing and marketplace plan data is unavailable right now.
            </Alert>
          </div>
        </Panel>
      )}

      {!isLoading && !isError && !hasResults && (
        <Panel>
          <EmptyState
            className="feature-panel-state"
            title="No paid plans configured"
            message="Pricing will appear here when paid marketplace listings or subscription-plan listings are active in the backend."
            actions={
              <Link className="feature-link" to={ROUTES.marketplace}>
                Browse free marketplace content
              </Link>
            }
          />
        </Panel>
      )}

      {subscriptionPlans.length > 0 && (
        <Panel title="Subscription plans" description="Subscription-plan listings configured in the backend.">
          <div className="feature-panel-body">
            {purchasesQuery.isError && (
              <Alert title="Current access could not be loaded" variant="warning">
                Pricing is available, but active purchase status could not be checked for this session.
              </Alert>
            )}
            <div className="feature-card-grid">
              {subscriptionPlans.map((listing) => (
                <PricingCard
                  disabledReason={session.status === 'anonymous' ? 'Sign in required' : disabledReason}
                  isCheckoutPending={checkout.isPending && activeListingId === listing.id}
                  key={listing.id}
                  listing={listing}
                  onCheckout={handleCheckout}
                  purchase={accessiblePurchasesByListingId.get(listing.id)}
                />
              ))}
            </div>
          </div>
        </Panel>
      )}

      {paidListings.length > 0 && (
        <Panel title="Paid marketplace content" description="Current paid listings with backend-provided prices and currencies.">
          <div className="feature-panel-body">
            {purchasesQuery.isError && !subscriptionPlans.length && (
              <Alert title="Current access could not be loaded" variant="warning">
                Pricing is available, but active purchase status could not be checked for this session.
              </Alert>
            )}
            <div className="feature-card-grid">
              {paidListings.map((listing) => (
                <PricingCard
                  disabledReason={session.status === 'anonymous' ? 'Sign in required' : disabledReason}
                  isCheckoutPending={checkout.isPending && activeListingId === listing.id}
                  key={listing.id}
                  listing={listing}
                  onCheckout={handleCheckout}
                  purchase={accessiblePurchasesByListingId.get(listing.id)}
                />
              ))}
            </div>
          </div>
        </Panel>
      )}

      {session.status === 'anonymous' && hasResults && (
        <Panel>
          <div className="feature-panel-body">
            <Alert title="Sign in to start checkout">
              Pricing is public. Payment creation requires a student account so the backend can attach
              purchases and access grants to the correct profile.
            </Alert>
          </div>
        </Panel>
      )}
    </PageContainer>
  )
}
