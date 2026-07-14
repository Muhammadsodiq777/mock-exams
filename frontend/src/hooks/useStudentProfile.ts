import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import { marketplaceKeys } from './queryKeys'

export function useStudentPurchases(enabled = true) {
  return useQuery({
    enabled,
    queryKey: marketplaceKeys.purchases(),
    queryFn: () => backendService.marketplace.listPurchases(),
  })
}

export function useStudentPayments(enabled = true) {
  return useQuery({
    enabled,
    queryKey: marketplaceKeys.payments,
    queryFn: () => backendService.marketplace.listPayments(),
  })
}
