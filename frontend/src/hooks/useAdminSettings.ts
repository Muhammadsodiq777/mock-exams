import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { PlatformSettingUDO } from '../types/backend'
import { adminKeys } from './queryKeys'

type UpdateSettingInput = {
  key: string
  payload: PlatformSettingUDO
}

export function useAdminSettings() {
  return useQuery({
    queryKey: adminKeys.settings,
    queryFn: () => backendService.admin.listSettings(),
  })
}

export function useAdminSetting(key?: string) {
  return useQuery({
    queryKey: key ? adminKeys.setting(key) : adminKeys.setting(''),
    queryFn: () => backendService.admin.getSetting(key as string),
    enabled: Boolean(key),
  })
}

export function useAdminSettingUpdate() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ key, payload }: UpdateSettingInput) =>
      backendService.admin.updateSetting(key, payload),
    onSuccess: (setting) => {
      void queryClient.invalidateQueries({ queryKey: adminKeys.settings })
      void queryClient.invalidateQueries({ queryKey: adminKeys.setting(setting.key) })
    },
  })
}
