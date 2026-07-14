import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  BackendId,
  FileRDO,
  SpeakingFeedbackRDO,
  SpeakingSubmissionCDO,
  SpeakingSubmissionRDO,
} from '../types/backend'
import { aiPracticeKeys } from './queryKeys'

export function useUploadSpeakingAudio() {
  return useMutation({
    mutationFn: (file: File) => {
      const payload = new FormData()
      payload.append('purpose', 'speaking_audio')
      payload.append('file', file)
      return backendService.files.uploadStudentFile(payload)
    },
  })
}

export function useCreateSpeakingSubmission() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: SpeakingSubmissionCDO) =>
      backendService.aiPractice.createSpeakingSubmission(payload),
    onSuccess: (submission) => {
      queryClient.setQueryData(aiPracticeKeys.speakingSubmission(submission.id), submission)
    },
  })
}

export function useSpeakingSubmission(submissionId?: BackendId | null, poll = false) {
  return useQuery({
    enabled: Boolean(submissionId),
    queryKey: submissionId
      ? aiPracticeKeys.speakingSubmission(submissionId)
      : aiPracticeKeys.speakingSubmission(0),
    queryFn: () => backendService.aiPractice.getSpeakingSubmission(submissionId as BackendId),
    refetchInterval: (query) => {
      if (!poll) {
        return false
      }

      const submission = query.state.data as SpeakingSubmissionRDO | undefined
      return !submission ||
        submission.transcriptionStatus === 'queued' ||
        submission.transcriptionStatus === 'processing'
        ? 5_000
        : false
    },
  })
}

export function useStartSpeakingTranscription(submissionId?: BackendId | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (targetSubmissionId?: BackendId) =>
      backendService.aiPractice.startSpeakingTranscription(
        targetSubmissionId ?? (submissionId as BackendId),
      ),
    onSuccess: (submission) => {
      queryClient.setQueryData(aiPracticeKeys.speakingSubmission(submission.id), submission)
    },
  })
}

export function useStartSpeakingFeedback(submissionId?: BackendId | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (targetSubmissionId?: BackendId) =>
      backendService.aiPractice.startSpeakingFeedback(targetSubmissionId ?? (submissionId as BackendId)),
    onSuccess: (feedback) => {
      queryClient.setQueryData(aiPracticeKeys.speakingFeedback(feedback.submissionId), feedback)
      void queryClient.invalidateQueries({ queryKey: aiPracticeKeys.aiUsage })
    },
  })
}

export function useSpeakingFeedback(submissionId?: BackendId | null, enabled = false, poll = false) {
  return useQuery({
    enabled: Boolean(submissionId) && enabled,
    queryKey: submissionId
      ? aiPracticeKeys.speakingFeedback(submissionId)
      : aiPracticeKeys.speakingFeedback(0),
    queryFn: () => backendService.aiPractice.getSpeakingFeedback(submissionId as BackendId),
    refetchInterval: (query) => {
      if (!poll) {
        return false
      }

      const feedback = query.state.data as SpeakingFeedbackRDO | undefined
      return !feedback || feedback.status === 'queued' || feedback.status === 'processing'
        ? 5_000
        : false
    },
  })
}

export function useRetrySpeakingFeedback(submissionId?: BackendId | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => backendService.aiPractice.retrySpeakingFeedback(submissionId as BackendId),
    onSuccess: (feedback) => {
      queryClient.setQueryData(aiPracticeKeys.speakingFeedback(feedback.submissionId), feedback)
      void queryClient.invalidateQueries({ queryKey: aiPracticeKeys.aiUsage })
    },
  })
}

export type UploadedSpeakingAudio = Pick<FileRDO, 'contentType' | 'id' | 'sizeBytes'>
