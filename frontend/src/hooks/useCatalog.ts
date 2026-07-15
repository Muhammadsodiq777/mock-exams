import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { BackendId, TestCatalogRequest } from '../types/backend'
import { catalogKeys } from './queryKeys'

export function useCatalogTests(filters: TestCatalogRequest) {
  return useQuery({
    queryKey: catalogKeys.tests(filters),
    queryFn: () => backendService.catalog.listTests(filters),
  })
}

export function useCatalogTest(testId?: BackendId) {
  return useQuery({
    enabled: Boolean(testId),
    queryKey: testId ? catalogKeys.test(testId) : catalogKeys.test(0),
    queryFn: () => backendService.catalog.getTest(testId as BackendId),
  })
}

export function useTestCategories() {
  return useQuery({
    queryKey: catalogKeys.categories,
    queryFn: () => backendService.catalog.listCategories(),
  })
}

export function useTestTags() {
  return useQuery({
    queryKey: catalogKeys.tags,
    queryFn: () => backendService.catalog.listTags(),
  })
}

export function usePublicTeacherProfile(profileId?: BackendId) {
  return useQuery({
    enabled: Boolean(profileId),
    queryKey: profileId ? catalogKeys.teacherProfile(profileId) : catalogKeys.teacherProfile(0),
    queryFn: () => backendService.catalog.getTeacherProfile(profileId as BackendId),
  })
}
