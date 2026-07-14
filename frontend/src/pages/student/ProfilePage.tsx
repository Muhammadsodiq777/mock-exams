import { Link } from 'react-router-dom'

import {
  Alert,
  Badge,
  DataTable,
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
import { useStudentPayments, useStudentPurchases } from '../../hooks/useStudentProfile'
import { useSessionStore } from '../../store/sessionStore'
import type { PaymentRDO, PurchaseRDO } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

const purchaseStatusTone = (purchase: PurchaseRDO) => {
  if (purchase.accessStatus.accessible) {
    return 'success'
  }

  if (purchase.status === 'refunded' || purchase.status === 'revoked') {
    return 'danger'
  }

  if (purchase.accessStatus.status === 'expired') {
    return 'warning'
  }

  return 'neutral'
}

const paymentStatusTone = (status: PaymentRDO['status']) => {
  if (status === 'paid') {
    return 'success'
  }

  if (status === 'failed' || status === 'cancelled' || status === 'refunded') {
    return 'danger'
  }

  return 'warning'
}

const errorMessage = (error: unknown, fallback: string) => normalizeApiError(error).message || fallback

export function ProfilePage() {
  const session = useSessionStore()
  const user = session.status === 'authenticated' ? session.user : null
  const purchasesQuery = useStudentPurchases()
  const paymentsQuery = useStudentPayments()
  const purchases = purchasesQuery.data?.items ?? []
  const payments = paymentsQuery.data?.items ?? []
  const activePurchases = purchases.filter((purchase) => purchase.accessStatus.accessible)
  const subscriptionPurchases = activePurchases.filter(
    (purchase) => purchase.listingType === 'subscription_plan' || purchase.sourceType === 'subscription',
  )
  const recentPurchases = purchases.slice(0, 5)
  const recentPayments = payments.slice(0, 5)

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Account"
        title="Profile"
        actions={
          <>
            <Link className="feature-link" to={ROUTES.dashboard}>
              Dashboard
            </Link>
            <Link className="feature-link feature-link--primary" to={ROUTES.pricing}>
              View pricing
            </Link>
          </>
        }
      />

      <div className="profile-grid">
        <Panel title="Account" description="Basic account details from your current session.">
          <div className="feature-panel-body">
            {user ? (
              <DescriptionList
                items={[
                  { label: 'Name', value: user.fullName },
                  { label: 'Email', value: user.email },
                  { label: 'Phone', value: user.phone ?? 'Not provided' },
                  { label: 'Language', value: user.preferredLanguage?.toUpperCase() ?? 'Not set' },
                  { label: 'Status', value: <Badge tone={user.status === 'active' ? 'success' : 'warning'}>{humanizeToken(user.status)}</Badge> },
                  { label: 'Roles', value: user.roles.map((role) => humanizeToken(role)).join(', ') },
                ]}
              />
            ) : (
              <Loader label="Loading account" />
            )}
          </div>
        </Panel>

        <Panel title="Access summary" description="Active grants and subscriptions known to the backend.">
          <div className="feature-panel-body">
            {purchasesQuery.isLoading && (
              <div className="feature-panel-state feature-panel-state--inline">
                <Loader label="Loading access" />
                <Skeleton lines={3} />
              </div>
            )}

            {purchasesQuery.isError && (
              <Alert title="Access could not be loaded" variant="danger">
                {errorMessage(purchasesQuery.error, 'Refresh the page and try again.')}
              </Alert>
            )}

            {!purchasesQuery.isLoading && !purchasesQuery.isError && (
              <div className="profile-stat-grid">
                <RepeatedItemCard>
                  <p className="profile-stat__label">Active access</p>
                  <p className="profile-stat__value">{activePurchases.length}</p>
                  <p className="feature-card__note">Granted purchases available now.</p>
                </RepeatedItemCard>
                <RepeatedItemCard>
                  <p className="profile-stat__label">Subscriptions</p>
                  <p className="profile-stat__value">{subscriptionPurchases.length}</p>
                  <p className="feature-card__note">Current subscription-plan access.</p>
                </RepeatedItemCard>
                <RepeatedItemCard>
                  <p className="profile-stat__label">Total purchases</p>
                  <p className="profile-stat__value">{purchases.length}</p>
                  <p className="feature-card__note">Free, paid, and admin-granted records.</p>
                </RepeatedItemCard>
              </div>
            )}
          </div>
        </Panel>
      </div>

      <Panel title="Active access" description="Content you can open right now.">
        <div className="feature-panel-body">
          {purchasesQuery.isLoading && <Loader label="Loading active access" />}
          {purchasesQuery.isError && (
            <Alert title="Active access could not be loaded" variant="danger">
              {errorMessage(purchasesQuery.error, 'Refresh the page and try again.')}
            </Alert>
          )}
          {!purchasesQuery.isLoading && !purchasesQuery.isError && activePurchases.length === 0 && (
            <EmptyState
              title="No active access yet"
              message="Free listings and completed paid purchases will appear here when access is granted."
              actions={
                <Link className="feature-link feature-link--primary" to={ROUTES.marketplace}>
                  Browse marketplace
                </Link>
              }
            />
          )}
          {activePurchases.length > 0 && (
            <div className="feature-card-grid feature-card-grid--compact">
              {activePurchases.map((purchase) => (
                <RepeatedItemCard key={purchase.id}>
                  <div className="feature-card__badges">
                    <Badge tone="success">Accessible</Badge>
                    <Badge tone="info">{humanizeToken(purchase.listingType)}</Badge>
                  </div>
                  <h3>{purchase.listingTitle}</h3>
                  <p className="feature-card__summary">
                    {formatCurrency(purchase.amount, purchase.currency)} · {humanizeToken(purchase.sourceType)}
                  </p>
                  <p className="feature-card__note">
                    {purchase.accessExpiresAt
                      ? `Access ends ${formatDate(purchase.accessExpiresAt)}`
                      : 'No expiry date reported.'}
                  </p>
                  <Link
                    className="feature-link"
                    to={fillRoute(ROUTES.marketplaceListing, { id: purchase.listingId })}
                  >
                    Open listing
                  </Link>
                </RepeatedItemCard>
              ))}
            </div>
          )}
        </div>
      </Panel>

      <div className="profile-grid profile-grid--history">
        <Panel title="Recent purchases" description="Purchase and access records from student APIs.">
          <DataTable
            columns={[
              {
                key: 'listing',
                header: 'Listing',
                render: (purchase) => (
                  <>
                    <strong>{purchase.listingTitle}</strong>
                    <p className="feature-card__note">{humanizeToken(purchase.listingType)}</p>
                  </>
                ),
              },
              {
                key: 'status',
                header: 'Status',
                render: (purchase) => (
                  <div className="profile-table-stack">
                    <Badge tone={purchaseStatusTone(purchase)}>{humanizeToken(purchase.accessStatus.status)}</Badge>
                    {purchase.accessStatus.reasonCode && (
                      <span>{humanizeToken(purchase.accessStatus.reasonCode)}</span>
                    )}
                  </div>
                ),
              },
              {
                key: 'amount',
                header: 'Amount',
                align: 'right',
                render: (purchase) => formatCurrency(purchase.amount, purchase.currency),
              },
              {
                key: 'date',
                header: 'Purchased',
                render: (purchase) => formatDate(purchase.purchasedAt),
              },
            ]}
            emptyMessage="Purchases, free access grants, and subscription grants will appear here."
            emptyTitle="No purchase history"
            error={purchasesQuery.isError ? errorMessage(purchasesQuery.error, 'Purchases could not be loaded.') : undefined}
            getRowKey={(purchase) => purchase.id}
            isLoading={purchasesQuery.isLoading}
            rows={recentPurchases}
          />
        </Panel>

        <Panel title="Recent payments" description="Provider-safe payment records for checkout attempts.">
          <DataTable
            columns={[
              {
                key: 'target',
                header: 'Target',
                render: (payment) => (
                  <>
                    <strong>{payment.targetListingTitle ?? humanizeToken(payment.targetType)}</strong>
                    <p className="feature-card__note">{humanizeToken(payment.provider)}</p>
                  </>
                ),
              },
              {
                key: 'status',
                header: 'Status',
                render: (payment) => (
                  <div className="profile-table-stack">
                    <Badge tone={paymentStatusTone(payment.status)}>{humanizeToken(payment.status)}</Badge>
                    {payment.failureCode && <span>{humanizeToken(payment.failureCode)}</span>}
                  </div>
                ),
              },
              {
                key: 'amount',
                header: 'Amount',
                align: 'right',
                render: (payment) => formatCurrency(payment.amount, payment.currency),
              },
              {
                key: 'date',
                header: 'Created',
                render: (payment) => formatDate(payment.createdAt),
              },
            ]}
            emptyMessage="Checkout attempts and paid confirmations will appear here."
            emptyTitle="No payment history"
            error={paymentsQuery.isError ? errorMessage(paymentsQuery.error, 'Payments could not be loaded.') : undefined}
            getRowKey={(payment) => payment.id}
            isLoading={paymentsQuery.isLoading}
            rows={recentPayments}
          />
        </Panel>
      </div>
    </PageContainer>
  )
}
