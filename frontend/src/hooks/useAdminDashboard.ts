import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { DateRangeFilter } from '../types/backend'
import { adminKeys } from './queryKeys'

export type AdminDashboardFilters = DateRangeFilter

export function useAdminDashboard(filters?: AdminDashboardFilters, enabled = true) {
  return useQuery({
    queryKey: adminKeys.dashboard(filters),
    queryFn: () => backendService.admin.getDashboard(filters),
    enabled,
  })
}
