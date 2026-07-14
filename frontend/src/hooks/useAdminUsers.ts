import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  AdminUserListRequest,
  AdminUserRDO,
  AdminUserStatusUDO,
  BackendId,
} from '../types/backend'
import { adminKeys } from './queryKeys'

type UpdateAdminUserStatusInput = {
  payload: AdminUserStatusUDO
  userId: BackendId
}

export function useAdminUsers(filters?: AdminUserListRequest) {
  return useQuery({
    queryKey: adminKeys.users(filters),
    queryFn: () => backendService.admin.listUsers(filters),
  })
}

export function useAdminUser(userId?: BackendId) {
  return useQuery({
    queryKey: userId ? adminKeys.user(userId) : adminKeys.user(0),
    queryFn: () => backendService.admin.getUser(userId as BackendId),
    enabled: Boolean(userId),
  })
}

export function useAdminUserStatusMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ payload, userId }: UpdateAdminUserStatusInput) =>
      backendService.admin.updateUserStatus(userId, payload),
    onSuccess: (user: AdminUserRDO) => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      void queryClient.invalidateQueries({ queryKey: adminKeys.user(user.id) })
    },
  })
}
