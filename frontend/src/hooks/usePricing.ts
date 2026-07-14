import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { MarketplaceListingRequest } from '../types/backend'
import { pricingKeys } from './queryKeys'

const pricingListingsRequest = {
  page: 0,
  price: 'paid',
  size: 12,
  sort: 'priceAmount,asc',
} satisfies MarketplaceListingRequest

const subscriptionPlansRequest = {
  page: 0,
  price: 'paid',
  size: 12,
  sort: 'priceAmount,asc',
  type: 'subscription_plan',
} satisfies MarketplaceListingRequest

export function usePricingListings() {
  return useQuery({
    queryKey: pricingKeys.listings(pricingListingsRequest),
    queryFn: () => backendService.marketplace.listPublicListings(pricingListingsRequest),
  })
}

export function useSubscriptionPlans() {
  return useQuery({
    queryKey: pricingKeys.listings(subscriptionPlansRequest),
    queryFn: () => backendService.marketplace.listPublicListings(subscriptionPlansRequest),
  })
}
