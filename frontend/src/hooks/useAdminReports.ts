import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { AdminReportName, AdminReportRequest } from '../types/backend'
import { adminKeys } from './queryKeys'

export function useAdminReport(name: AdminReportName, filters?: AdminReportRequest, enabled = true) {
  return useQuery({
    queryKey: adminKeys.reports(name, filters),
    queryFn: () => backendService.admin.getReport(name, filters),
    enabled,
  })
}
