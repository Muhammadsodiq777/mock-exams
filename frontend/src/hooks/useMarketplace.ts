import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, MarketplaceListingRequest } from '../types/backend'
import { marketplaceKeys } from './queryKeys'

export function useMarketplaceListings(filters: MarketplaceListingRequest) {
  return useQuery({
    queryKey: marketplaceKeys.publicListings(filters),
    queryFn: () => backendService.marketplace.listPublicListings(filters),
  })
}

export function useMarketplaceListing(listingId?: BackendId) {
  return useQuery({
    enabled: Boolean(listingId),
    queryKey: listingId ? marketplaceKeys.publicListing(listingId) : marketplaceKeys.publicListing(0),
    queryFn: () => backendService.marketplace.getPublicListing(listingId as BackendId),
  })
}

export function useListingAccess(listingId?: BackendId, enabled = true) {
  return useQuery({
    enabled: Boolean(listingId) && enabled,
    queryKey: listingId ? marketplaceKeys.listingAccess(listingId) : marketplaceKeys.listingAccess(0),
    queryFn: () => backendService.marketplace.getListingAccess(listingId as BackendId),
  })
}

export function useAccessibleListingItems(listingId?: BackendId, enabled = true) {
  return useQuery({
    enabled: Boolean(listingId) && enabled,
    queryKey: listingId ? marketplaceKeys.listingItems(listingId) : marketplaceKeys.listingItems(0),
    queryFn: () => backendService.marketplace.listAccessibleItems(listingId as BackendId),
  })
}

export function useGrantFreeAccess(listingId: BackendId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => backendService.marketplace.grantFreeAccess(listingId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.all })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.publicListing(listingId) })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.listingAccess(listingId) })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.listingItems(listingId) })
    },
  })
}

