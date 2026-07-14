import { useCallback } from 'react'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  BackendId,
  TeacherListeningSegmentCDO,
  TeacherListeningSegmentReorderUDO,
  TeacherListeningSegmentUDO,
  TeacherQuestionCDO,
  TeacherQuestionUDO,
  TeacherReadingParagraphCDO,
  TeacherReadingParagraphReorderUDO,
  TeacherReadingParagraphUDO,
  TeacherTestCDO,
  TeacherTestListRequest,
  TeacherTestRDO,
  TeacherTestSectionCDO,
  TeacherTestSectionReorderUDO,
  TeacherTestSectionUDO,
  TeacherTestUDO,
} from '../types/backend'
import { teacherKeys } from './queryKeys'

type SectionMutationInput<TPayload> = {
  sectionId: BackendId
  payload: TPayload
}

type ParagraphMutationInput<TPayload> = {
  sectionId: BackendId
  paragraphId: BackendId
  payload: TPayload
}

type SegmentMutationInput<TPayload> = {
  sectionId: BackendId
  segmentId: BackendId
  payload: TPayload
}

type QuestionMutationInput<TPayload> = {
  sectionId: BackendId
  questionId: BackendId
  payload: TPayload
}

export const readonlyTeacherTestStatuses = new Set([
  'submitted',
  'published',
  'archived',
])

export const canEditTeacherTest = (test?: Pick<TeacherTestRDO, 'visibility'> | null) =>
  Boolean(test && !readonlyTeacherTestStatuses.has(String(test.visibility)))

export function useTeacherTestPreview(testId?: BackendId) {
  return useQuery({
    queryKey: testId ? teacherKeys.testPreview(testId) : teacherKeys.testPreview(0),
    queryFn: () => backendService.teacher.getTestPreview(testId as BackendId),
    enabled: Boolean(testId),
  })
}

export function useTeacherTests(filters?: TeacherTestListRequest) {
  return useQuery({
    queryKey: teacherKeys.tests(filters),
    queryFn: () => backendService.teacher.listTests(filters),
  })
}

export function useTeacherTestBuilderMutations(testId?: BackendId) {
  const queryClient = useQueryClient()

  const invalidateTeacherTests = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: teacherKeys.testsRoot })
    void queryClient.invalidateQueries({ queryKey: teacherKeys.dashboard })
  }, [queryClient])

  const setTestData = useCallback(
    (test: TeacherTestRDO) => {
      queryClient.setQueryData(teacherKeys.testPreview(test.id), test)
      queryClient.setQueryData(teacherKeys.test(test.id), test)
      invalidateTeacherTests()
    },
    [invalidateTeacherTests, queryClient],
  )

  const requireTestId = useCallback(() => {
    if (!testId) {
      throw new Error('A saved teacher test is required for this builder action.')
    }

    return testId
  }, [testId])

  const createTest = useMutation({
    mutationFn: (payload: TeacherTestCDO) => backendService.teacher.createTest(payload),
    onSuccess: setTestData,
  })

  const updateTest = useMutation({
    mutationFn: (payload: TeacherTestUDO) => backendService.teacher.updateTest(requireTestId(), payload),
    onSuccess: setTestData,
  })

  const submitTest = useMutation({
    mutationFn: () => backendService.teacher.submitTest(requireTestId()),
    onSuccess: setTestData,
  })

  const archiveTest = useMutation({
    mutationFn: () => backendService.teacher.archiveTest(requireTestId()),
    onSuccess: setTestData,
  })

  const addSection = useMutation({
    mutationFn: (payload: TeacherTestSectionCDO) =>
      backendService.teacher.addTestSection(requireTestId(), payload),
    onSuccess: setTestData,
  })

  const updateSection = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherTestSectionUDO>) =>
      backendService.teacher.updateTestSection(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const reorderSections = useMutation({
    mutationFn: (payload: TeacherTestSectionReorderUDO) =>
      backendService.teacher.reorderTestSections(requireTestId(), payload),
    onSuccess: setTestData,
  })

  const deleteSection = useMutation({
    mutationFn: (sectionId: BackendId) => backendService.teacher.deleteTestSection(requireTestId(), sectionId),
    onSuccess: setTestData,
  })

  const addParagraph = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherReadingParagraphCDO>) =>
      backendService.teacher.addReadingParagraph(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const updateParagraph = useMutation({
    mutationFn: ({ sectionId, paragraphId, payload }: ParagraphMutationInput<TeacherReadingParagraphUDO>) =>
      backendService.teacher.updateReadingParagraph(requireTestId(), sectionId, paragraphId, payload),
    onSuccess: setTestData,
  })

  const reorderParagraphs = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherReadingParagraphReorderUDO>) =>
      backendService.teacher.reorderReadingParagraphs(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const deleteParagraph = useMutation({
    mutationFn: ({ sectionId, paragraphId }: Omit<ParagraphMutationInput<never>, 'payload'>) =>
      backendService.teacher.deleteReadingParagraph(requireTestId(), sectionId, paragraphId),
    onSuccess: setTestData,
  })

  const addListeningSegment = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherListeningSegmentCDO>) =>
      backendService.teacher.addListeningSegment(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const updateListeningSegment = useMutation({
    mutationFn: ({ sectionId, segmentId, payload }: SegmentMutationInput<TeacherListeningSegmentUDO>) =>
      backendService.teacher.updateListeningSegment(requireTestId(), sectionId, segmentId, payload),
    onSuccess: setTestData,
  })

  const reorderListeningSegments = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherListeningSegmentReorderUDO>) =>
      backendService.teacher.reorderListeningSegments(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const deleteListeningSegment = useMutation({
    mutationFn: ({ sectionId, segmentId }: Omit<SegmentMutationInput<never>, 'payload'>) =>
      backendService.teacher.deleteListeningSegment(requireTestId(), sectionId, segmentId),
    onSuccess: setTestData,
  })

  const addQuestion = useMutation({
    mutationFn: ({ sectionId, payload }: SectionMutationInput<TeacherQuestionCDO>) =>
      backendService.teacher.addQuestion(requireTestId(), sectionId, payload),
    onSuccess: setTestData,
  })

  const updateQuestion = useMutation({
    mutationFn: ({ sectionId, questionId, payload }: QuestionMutationInput<TeacherQuestionUDO>) =>
      backendService.teacher.updateQuestion(requireTestId(), sectionId, questionId, payload),
    onSuccess: setTestData,
  })

  const deleteQuestion = useMutation({
    mutationFn: ({ sectionId, questionId }: Omit<QuestionMutationInput<never>, 'payload'>) =>
      backendService.teacher.deleteQuestion(requireTestId(), sectionId, questionId),
    onSuccess: setTestData,
  })

  return {
    addListeningSegment,
    addParagraph,
    addQuestion,
    addSection,
    archiveTest,
    createTest,
    deleteListeningSegment,
    deleteParagraph,
    deleteQuestion,
    deleteSection,
    reorderListeningSegments,
    reorderParagraphs,
    reorderSections,
    submitTest,
    updateListeningSegment,
    updateParagraph,
    updateQuestion,
    updateSection,
    updateTest,
  }
}
