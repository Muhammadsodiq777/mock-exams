import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  AdminModerationTestDetailRDO,
  AdminModerationTestListRequest,
  BackendId,
  TestModerationRejectCDO,
} from '../types/backend'
import { adminKeys } from './queryKeys'

type RejectModerationInput = {
  payload: TestModerationRejectCDO
  testId: BackendId
}

export function useAdminModerationTests(filters?: AdminModerationTestListRequest) {
  return useQuery({
    queryKey: adminKeys.moderationTests(filters),
    queryFn: () => backendService.admin.listModerationTests(filters),
  })
}

export function useAdminModerationMeta() {
  return useQuery({
    queryKey: adminKeys.moderationTestsMeta,
    queryFn: () => backendService.admin.getModerationMeta(),
  })
}

export function useAdminModerationTest(testId?: BackendId) {
  return useQuery({
    queryKey: testId ? adminKeys.moderationTest(testId) : adminKeys.moderationTest(0),
    queryFn: () => backendService.admin.getModerationTest(testId as BackendId),
    enabled: Boolean(testId),
  })
}

export function useAdminModerationDecisions() {
  const queryClient = useQueryClient()

  const invalidateModeration = (detail: AdminModerationTestDetailRDO) => {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['admin', 'moderation'] })
    void queryClient.invalidateQueries({ queryKey: adminKeys.moderationTest(detail.test.id) })
  }

  const approve = useMutation({
    mutationFn: (testId: BackendId) => backendService.admin.approveModerationTest(testId),
    onSuccess: invalidateModeration,
  })

  const reject = useMutation({
    mutationFn: ({ payload, testId }: RejectModerationInput) =>
      backendService.admin.rejectModerationTest(testId, payload),
    onSuccess: invalidateModeration,
  })

  return { approve, reject }
}
