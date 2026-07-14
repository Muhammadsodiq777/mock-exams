import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId } from '../types/backend'
import { fileKeys } from './queryKeys'

export function useFileDownloadUrl(fileId?: BackendId | null, enabled = true) {
  return useQuery({
    enabled: Boolean(fileId) && enabled,
    queryKey: fileId ? fileKeys.downloadUrl(fileId) : fileKeys.downloadUrl(0),
    queryFn: () => backendService.files.getDownloadUrl(fileId as BackendId),
    staleTime: 60_000,
  })
}
