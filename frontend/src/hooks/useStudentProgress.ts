import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import { aiPracticeKeys, resultKeys } from './queryKeys'

export function useStudentProgress(days = 180) {
  const filters = { days }

  return useQuery({
    queryKey: resultKeys.progress(filters),
    queryFn: () => backendService.results.getStudentProgress(filters),
  })
}

export function useActiveStudyPlans() {
  return useQuery({
    queryKey: aiPracticeKeys.activeStudyPlan,
    queryFn: () => backendService.aiPractice.getActiveStudyPlan(),
  })
}

export function useAiUsage(enabled = true) {
  return useQuery({
    enabled,
    queryKey: aiPracticeKeys.aiUsage,
    queryFn: () => backendService.aiPractice.getAiUsage(),
  })
}
