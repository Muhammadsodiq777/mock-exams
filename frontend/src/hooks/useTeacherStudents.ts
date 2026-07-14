import { useQuery } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type { PageRequest } from '../types/backend'
import { teacherKeys } from './queryKeys'

export type TeacherStudentActivityRequest = PageRequest & {
  rating?: number
  testId?: number
}

export function useTeacherStudentActivity(filters?: TeacherStudentActivityRequest) {
  return useQuery({
    queryKey: teacherKeys.reviews(filters),
    queryFn: () => backendService.teacher.listReviews(filters),
  })
}
