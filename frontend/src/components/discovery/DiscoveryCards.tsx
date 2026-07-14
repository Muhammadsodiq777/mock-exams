import { Link } from 'react-router-dom'

import { fillRoute, ROUTES } from '../../constants/routes'
import type { MarketplaceListingSummaryRDO, TestCardRDO } from '../../types/backend'
import { formatCurrency, formatMinutes, formatNumber, humanizeToken } from '../../utils/formatters'
import { Badge, DescriptionList, RepeatedItemCard } from '../ui'

const joinLabels = (items: Array<{ name: string }>) =>
  items.length ? items.map((item) => item.name).join(', ') : 'General'

export function TestCard({ test }: { test: TestCardRDO }) {
  const band =
    test.estimatedBandMin || test.estimatedBandMax
      ? `${formatNumber(test.estimatedBandMin, '?')}-${formatNumber(test.estimatedBandMax, '?')}`
      : 'Adaptive'

  return (
    <RepeatedItemCard className="discovery-card">
      <div className="feature-card__badges">
        <Badge tone={test.free ? 'success' : 'warning'}>
          {test.free ? 'Free' : formatCurrency(test.priceAmount, test.currency, 'Paid')}
        </Badge>
        <Badge tone="info">{humanizeToken(test.type)}</Badge>
      </div>
      <h3>{test.title}</h3>
      {test.description && <p className="feature-card__summary">{test.description}</p>}
      <DescriptionList
        items={[
          { label: 'Band', value: band },
          { label: 'Duration', value: formatMinutes(test.durationMinutes) },
          { label: 'Difficulty', value: humanizeToken(test.difficulty, 'Any level') },
          { label: 'Language', value: test.language.toUpperCase() },
          { label: 'Teacher', value: test.teacher?.displayName ?? 'Official' },
          { label: 'Category', value: joinLabels(test.categories) },
        ]}
      />
      <div className="feature-card__tags">
        {test.tags.slice(0, 4).map((tag) => (
          <span key={tag.id}>{tag.name}</span>
        ))}
      </div>
      <div className="feature-card__actions">
        <Link className="feature-link feature-link--primary" to={fillRoute(ROUTES.testDetail, { id: test.id })}>
          {test.type === 'reading' ? 'Open reading test' : 'View details'}
        </Link>
      </div>
    </RepeatedItemCard>
  )
}

export function MarketplaceCard({ listing }: { listing: MarketplaceListingSummaryRDO }) {
  const isAccessible = listing.accessStatus?.accessible
  const isLiveSpeaking = listing.listingType === 'speaking_question_pack'

  return (
    <RepeatedItemCard className="discovery-card">
      <div className="feature-card__badges">
        <Badge tone={listing.free ? 'success' : 'warning'}>
          {listing.free ? 'Free' : formatCurrency(listing.priceAmount, listing.currency, 'Paid')}
        </Badge>
        <Badge tone={isLiveSpeaking ? 'warning' : 'info'}>{humanizeToken(listing.listingType)}</Badge>
        {isAccessible && <Badge tone="success">Access granted</Badge>}
      </div>
      <h3>{listing.title}</h3>
      {listing.description && <p className="feature-card__summary">{listing.description}</p>}
      <DescriptionList
        items={[
          { label: 'Teacher', value: listing.teacher.displayName },
          { label: 'Items', value: listing.itemCount },
          { label: 'Language', value: listing.language.toUpperCase() },
          { label: 'Category', value: joinLabels(listing.categories) },
        ]}
      />
      <div className="feature-card__tags">
        {listing.tags.slice(0, 4).map((tag) => (
          <span key={tag.id}>{tag.name}</span>
        ))}
      </div>
      {isLiveSpeaking && (
        <p className="feature-card__note">
          Live speaking booking is a scheduled flow and will open from the schedule module.
        </p>
      )}
      <div className="feature-card__actions">
        <Link
          className="feature-link feature-link--primary"
          to={`${ROUTES.marketplace}/${listing.id}`}
        >
          {isAccessible ? 'Open content' : listing.free ? 'Get free access' : 'View purchase options'}
        </Link>
      </div>
    </RepeatedItemCard>
  )
}

