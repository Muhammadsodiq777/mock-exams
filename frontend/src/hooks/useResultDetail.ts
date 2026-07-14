import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId } from '../types/backend'
import { resultKeys } from './queryKeys'

export function useResultDetail(resultId?: BackendId) {
  return useQuery({
    enabled: Boolean(resultId),
    queryKey: resultId ? resultKeys.detail(resultId) : resultKeys.detail(0),
    queryFn: () => backendService.results.getResult(resultId as BackendId),
  })
}
