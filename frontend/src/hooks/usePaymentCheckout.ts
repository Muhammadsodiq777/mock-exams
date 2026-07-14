import { useMutation, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, PaymentCDO } from '../types/backend'
import { marketplaceKeys, pricingKeys } from './queryKeys'

type MarketplaceCheckoutVariables = {
  listingId: BackendId
  promoCode?: string | null
  returnUrl?: string | null
}

const currentBrowserUrl = () => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.location.href
}

export function isBrowserCheckoutRedirect(url?: string | null) {
  if (!url) {
    return false
  }

  return url.startsWith('http://') || url.startsWith('https://')
}

export function useCreateMarketplaceCheckout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ listingId, promoCode, returnUrl }: MarketplaceCheckoutVariables) => {
      const payload: PaymentCDO = {
        targetType: 'marketplace_listing',
        listingId,
        promoCode: promoCode?.trim() || null,
        returnUrl: returnUrl ?? currentBrowserUrl(),
      }

      return backendService.marketplace.createPayment(payload)
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.payments })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.purchases() })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.listingAccess(variables.listingId) })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.publicListing(variables.listingId) })
      void queryClient.invalidateQueries({ queryKey: pricingKeys.all })
    },
  })
}
