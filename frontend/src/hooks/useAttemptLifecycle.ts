import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, StudentAnswerUDO } from '../types/backend'
import { attemptKeys, resultKeys } from './queryKeys'

export function useStartAttempt(testId: BackendId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => backendService.attempts.startAttempt(testId),
    onSuccess: (attempt) => {
      queryClient.setQueryData(attemptKeys.detail(attempt.id), attempt)
    },
  })
}

export function useSaveAnswer(attemptId?: BackendId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: StudentAnswerUDO) =>
      backendService.attempts.saveAnswer(attemptId as BackendId, payload),
    onSuccess: () => {
      if (attemptId) {
        void queryClient.invalidateQueries({ queryKey: attemptKeys.detail(attemptId) })
      }
    },
  })
}

export function useSubmitAttempt(attemptId?: BackendId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => backendService.attempts.submitAttempt(attemptId as BackendId),
    onSuccess: (attempt) => {
      queryClient.setQueryData(attemptKeys.detail(attempt.id), attempt)
      void queryClient.invalidateQueries({ queryKey: resultKeys.all })
      void queryClient.invalidateQueries({ queryKey: resultKeys.progress() })
    },
  })
}

export function useAttemptResult(attemptId?: BackendId, enabled = true) {
  return useQuery({
    enabled: Boolean(attemptId) && enabled,
    queryKey: attemptId ? attemptKeys.result(attemptId) : attemptKeys.result(0),
    queryFn: () => backendService.results.getAttemptResult(attemptId as BackendId),
  })
}

