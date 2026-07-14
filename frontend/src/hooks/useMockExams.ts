import { useQuery } from '@tanstack/react-query'

import { mockExamKeys } from './queryKeys'
import { backendService } from '../services/backend.service'

export function useMockExams() {
  return useQuery({
    queryKey: mockExamKeys.all,
    queryFn: backendService.listExams,
  })
}
