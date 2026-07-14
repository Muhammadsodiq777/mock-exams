import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, NotificationRDO, PageRequest } from '../types/backend'
import { teacherKeys } from './queryKeys'

export function useTeacherNotifications(filters?: PageRequest) {
  return useQuery({
    queryKey: teacherKeys.notifications(filters),
    queryFn: () => backendService.teacher.listNotifications(filters),
  })
}

export function useMarkTeacherNotificationRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (notificationId: BackendId) =>
      backendService.teacher.markNotificationRead(notificationId),
    onSuccess: (notification: NotificationRDO) => {
      void queryClient.invalidateQueries({ queryKey: teacherKeys.notificationsRoot })
      void queryClient.invalidateQueries({ queryKey: teacherKeys.dashboard })
      void queryClient.invalidateQueries({ queryKey: teacherKeys.notificationUnreadCount })
      void queryClient.invalidateQueries({
        queryKey: teacherKeys.notification(notification.id),
      })
    },
  })
}
