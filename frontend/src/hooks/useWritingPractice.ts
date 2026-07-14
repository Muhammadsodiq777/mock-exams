import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, WritingFeedbackRDO, WritingSubmissionCDO } from '../types/backend'
import { aiPracticeKeys } from './queryKeys'

export function useCreateWritingSubmission() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: WritingSubmissionCDO) =>
      backendService.aiPractice.createWritingSubmission(payload),
    onSuccess: (submission) => {
      queryClient.setQueryData(aiPracticeKeys.writingSubmission(submission.id), submission)
      void queryClient.invalidateQueries({ queryKey: aiPracticeKeys.aiUsage })
    },
  })
}

export function useWritingSubmission(submissionId?: BackendId | null) {
  return useQuery({
    enabled: Boolean(submissionId),
    queryKey: submissionId
      ? aiPracticeKeys.writingSubmission(submissionId)
      : aiPracticeKeys.writingSubmission(0),
    queryFn: () => backendService.aiPractice.getWritingSubmission(submissionId as BackendId),
  })
}

export function useWritingFeedback(submissionId?: BackendId | null, poll = false) {
  return useQuery({
    enabled: Boolean(submissionId),
    queryKey: submissionId
      ? aiPracticeKeys.writingFeedback(submissionId)
      : aiPracticeKeys.writingFeedback(0),
    queryFn: () => backendService.aiPractice.getWritingFeedback(submissionId as BackendId),
    refetchInterval: (query) => {
      if (!poll) {
        return false
      }

      const feedback = query.state.data as WritingFeedbackRDO | undefined
      return !feedback || feedback.status === 'queued' || feedback.status === 'processing'
        ? 5_000
        : false
    },
  })
}

export function useRetryWritingFeedback(submissionId?: BackendId | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => backendService.aiPractice.retryWritingFeedback(submissionId as BackendId),
    onSuccess: (feedback) => {
      queryClient.setQueryData(aiPracticeKeys.writingFeedback(feedback.submissionId), feedback)
      void queryClient.invalidateQueries({ queryKey: aiPracticeKeys.aiUsage })
    },
  })
}
