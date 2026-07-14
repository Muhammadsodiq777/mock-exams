import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  AdminTeacherProfileListRequest,
  BackendId,
  TeacherProfileModerationUDO,
  TeacherProfileRDO,
} from '../types/backend'
import { adminKeys } from './queryKeys'

type RejectTeacherProfileInput = {
  profileId: BackendId
  payload: TeacherProfileModerationUDO
}

export function useAdminTeacherProfiles(filters?: AdminTeacherProfileListRequest) {
  return useQuery({
    queryKey: adminKeys.teacherProfiles(filters),
    queryFn: () => backendService.admin.listTeacherProfiles(filters),
  })
}

export function useAdminTeacherProfile(profileId?: BackendId) {
  return useQuery({
    queryKey: profileId ? adminKeys.teacherProfile(profileId) : adminKeys.teacherProfile(0),
    queryFn: () => backendService.admin.getTeacherProfile(profileId as BackendId),
    enabled: Boolean(profileId),
  })
}

export function useAdminTeacherProfileModeration() {
  const queryClient = useQueryClient()

  const updateProfileCaches = (profile: TeacherProfileRDO) => {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['admin', 'teacher-profiles'] })
    void queryClient.invalidateQueries({ queryKey: adminKeys.teacherProfile(profile.id) })
  }

  const approve = useMutation({
    mutationFn: (profileId: BackendId) => backendService.admin.approveTeacherProfile(profileId),
    onSuccess: updateProfileCaches,
  })

  const reject = useMutation({
    mutationFn: ({ payload, profileId }: RejectTeacherProfileInput) =>
      backendService.admin.rejectTeacherProfile(profileId, payload),
    onSuccess: updateProfileCaches,
  })

  return { approve, reject }
}
