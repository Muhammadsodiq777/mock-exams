export type BackendId = number

export type BackendTimestamp = string

export type BackendDate = string

export type ApiRouteArea = 'public' | 'student' | 'teacher' | 'admin' | 'auth'

export type ApiRoutePrefix = '/api/v1' | `/api/v1/${ApiRouteArea}`

export type RoleIdentifier = 'student' | 'teacher' | 'admin'

export type AdminUserStatus = 'active' | 'suspended' | 'banned' | 'deleted'

export type AuthTokenType = 'Bearer'

export type SortDirection = 'asc' | 'desc'

export type SortQueryValue = `${string},${SortDirection}` | string

export type PageRequest = {
  page?: number
  size?: number
  sort?: SortQueryValue | SortQueryValue[]
}

export type DateRangeFilter = {
  from?: BackendTimestamp
  to?: BackendTimestamp
}

export type SearchFilter = {
  q?: string
}

export type StatusFilter<TStatus extends string = string> = {
  status?: TStatus | TStatus[]
}

export type ListRequest<TFilter extends Record<string, unknown> = Record<string, never>> =
  PageRequest & SearchFilter & TFilter

/**
 * Backend DTO suffix convention:
 * - CDO: create data object sent from client to backend.
 * - UDO: update data object sent from client to backend.
 * - RDO: Response Data Object returned from backend to client.
 */
export type BackendCDO<TPayload extends Record<string, unknown>> = TPayload

export type BackendUDO<TPayload extends Record<string, unknown>> = TPayload

export type BackendRDO<TPayload extends Record<string, unknown>> = TPayload

export type ApiSuccessMeta = {
  requestId: string
}

export type ApiSuccessEnvelope<
  TData,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
> = {
  data: TData
  meta: TMeta
}

export type PageMetadata = {
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type FilterMetadata = Record<string, unknown>

export type QueueMetadata = {
  pendingCount: number
  oldestSubmittedAt?: BackendTimestamp | null
}

export type DashboardMetadata = ApiSuccessMeta & {
  asOf?: BackendTimestamp
  stale?: boolean
  generatedBy?: string
}

export type CommandStatus = 'accepted' | 'completed' | 'queued' | 'noop'

export type CommandResponseRDO = BackendRDO<{
  status: CommandStatus
  message?: string | null
  resourceId?: BackendId | null
  jobId?: BackendId | string | null
}>

export type ApiListMeta = ApiSuccessMeta & {
  page: PageMetadata
  sort?: SortMetadata[]
  filters?: FilterMetadata
}

export type ApiListResponse<TItem, TMeta extends ApiListMeta = ApiListMeta> =
  ApiSuccessEnvelope<TItem[], TMeta>

export type ApiUnpagedListResponse<
  TItem,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
> = ApiSuccessEnvelope<TItem[], TMeta>

export type ApiPageMeta = ApiListMeta

export type ApiPageResponse<TItem> = ApiSuccessEnvelope<TItem[], ApiPageMeta>

export type ApiQueueListMeta = ApiListMeta & {
  queue: QueueMetadata
}

export type ApiQueueListResponse<TItem> = ApiSuccessEnvelope<TItem[], ApiQueueListMeta>

export type ApiDashboardResponse<
  TDashboard,
  TMeta extends DashboardMetadata = DashboardMetadata,
> = ApiSuccessEnvelope<TDashboard, TMeta>

export type ApiCommandResponse<TData = CommandResponseRDO | null> = ApiSuccessEnvelope<TData>

export type ApiResourceResult<TData, TMeta extends ApiSuccessMeta = ApiSuccessMeta> = {
  data: TData
  meta: TMeta
}

export type ApiListResult<TItem, TMeta extends ApiListMeta = ApiListMeta> = {
  items: TItem[]
  meta: TMeta
}

export type ApiUnpagedListResult<
  TItem,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
> = {
  items: TItem[]
  meta: TMeta
}

export type ApiDashboardResult<
  TDashboard,
  TMeta extends DashboardMetadata = DashboardMetadata,
> = {
  dashboard: TDashboard
  meta: TMeta
}

export type ApiCommandResult<TData = CommandResponseRDO | null> = {
  data: TData
  meta: ApiSuccessMeta
}

export type SortMetadata = {
  field: string
  direction: SortDirection
}

export type ApiFieldError = {
  field: string
  code: string
  message: string
}

export type ApiErrorCode =
  | 'VALIDATION_FAILED'
  | 'AUTHENTICATION_REQUIRED'
  | 'TOKEN_INVALID'
  | 'TOKEN_EXPIRED'
  | 'REFRESH_TOKEN_INVALID'
  | 'ACCOUNT_INACTIVE'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'EMAIL_ALREADY_REGISTERED'
  | 'UNKNOWN_SETTING'
  | 'PLATFORM_SETTING_INVALID'
  | 'INTEGRATION_CONFIGURATION_MISSING'
  | 'TEACHER_PROFILE_STATUS_CONFLICT'
  | 'LISTING_NOT_FOUND'
  | 'LISTING_STATUS_CONFLICT'
  | 'LISTING_ITEM_INVALID'
  | 'LISTING_NOT_ACCESSIBLE'
  | 'PAYMENT_REQUIRED'
  | 'PAYMENT_NOT_FOUND'
  | 'PAYMENT_PROVIDER_DISABLED'
  | 'PAYMENT_SIGNATURE_INVALID'
  | 'PAYMENT_TARGET_UNSUPPORTED'
  | 'PURCHASE_NOT_FOUND'
  | 'ALREADY_PURCHASED'
  | 'ACCESS_EXPIRED'
  | 'TEACHER_PROFILE_NOT_FOUND'
  | 'TEST_NOT_FOUND'
  | 'TEST_SECTION_NOT_FOUND'
  | 'TEST_STATUS_CONFLICT'
  | 'TEACHER_TEST_STATUS_CONFLICT'
  | 'READING_PARAGRAPH_NOT_FOUND'
  | 'LISTENING_SEGMENT_NOT_FOUND'
  | 'ATTEMPT_NOT_FOUND'
  | 'ATTEMPT_ALREADY_SUBMITTED'
  | 'ATTEMPT_EXPIRED'
  | 'ATTEMPT_STATUS_CONFLICT'
  | 'ATTEMPT_NOT_SUBMITTED'
  | 'QUESTION_NOT_FOUND'
  | 'QUESTION_OPTION_NOT_FOUND'
  | 'RESULT_NOT_FOUND'
  | 'RESULT_NOT_READY'
  | 'AI_USAGE_LIMIT_EXCEEDED'
  | 'AI_FEEDBACK_NOT_FOUND'
  | 'AI_FEEDBACK_NOT_READY'
  | 'AI_PROVIDER_FAILED'
  | 'UNSUPPORTED_WRITING_TASK_TYPE'
  | 'TRANSCRIPT_REQUIRED'
  | 'WALLET_CURRENCY_MISMATCH'
  | 'WALLET_INSUFFICIENT_BALANCE'
  | 'WITHDRAWAL_NOT_FOUND'
  | 'WITHDRAWAL_INVALID_STATUS'
  | 'WITHDRAWAL_STATUS_CONFLICT'
  | 'PAYMENT_DISPUTE_NOT_FOUND'
  | 'PAYMENT_DISPUTE_STATUS_NOT_ALLOWED'
  | 'PAYMENT_DISPUTE_ALREADY_OPEN'
  | 'PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION'
  | 'TEST_MODERATION_NOT_READY'
  | 'PROMO_CODE_NOT_FOUND'
  | 'PROMO_CODE_INVALID'
  | 'REVIEW_NOT_FOUND'
  | 'REVIEW_NOT_ALLOWED'
  | 'DISPUTE_NOT_FOUND'
  | 'DISPUTE_STATUS_CONFLICT'
  | 'FILE_NOT_FOUND'
  | 'UNSUPPORTED_FILE_PURPOSE'
  | 'SCHEDULE_SLOT_NOT_FOUND'
  | 'SCHEDULE_SLOT_UNAVAILABLE'
  | 'SCHEDULE_BOOKING_NOT_FOUND'
  | 'SCHEDULE_BOOKING_STATUS_CONFLICT'
  | 'TEACHER_APPROVAL_REQUIRED'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR'
  | 'SERVICE_UNAVAILABLE'

export type ApiErrorEnvelope<TCode extends string = ApiErrorCode> = {
  error: {
    code: TCode
    message: string
    fieldErrors?: ApiFieldError[]
    details?: Record<string, unknown>
    requestId: string
  }
}

export type ApiResponse<
  TData,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
  TCode extends string = ApiErrorCode,
> =
  | ApiSuccessEnvelope<TData, TMeta>
  | ApiErrorEnvelope<TCode>

export type TeacherProfileStatus =
  | 'draft'
  | 'pending_approval'
  | 'approved'
  | 'rejected'
  | 'suspended'

export type CurrentUserRDO = BackendRDO<{
  id: BackendId
  email: string
  fullName: string
  phone: string | null
  status: 'active' | 'suspended' | 'banned' | 'deleted' | string
  preferredLanguage: string | null
  roles: RoleIdentifier[]
  teacherProfile?: {
    id: BackendId
    displayName: string
    bio?: string | null
    status: TeacherProfileStatus
  } | null
}>

export type AuthSessionRDO = BackendRDO<{
  accessToken: string
  tokenType: AuthTokenType
  expiresAt: BackendTimestamp
  user: CurrentUserRDO
}>

export type LoginCDO = BackendCDO<{
  email: string
  password: string
}>

export type RegisterStudentCDO = BackendCDO<{
  email: string
  password: string
  fullName: string
}>

export type RegisterTeacherCDO = BackendCDO<{
  email: string
  password: string
  fullName: string
  displayName: string
  bio: string
}>

export type MockExamRDO = {
  id: BackendId
  title: string
  subject: string
  durationMinutes: number
  questionCount: number
  createdAt: BackendTimestamp
}

export type TeacherProfileRDO = BackendRDO<{
  id: BackendId
  userId: BackendId
  displayName: string
  headline?: string | null
  bio: string
  avatarFileId?: BackendId | null
  status: TeacherProfileStatus
  ratingAverage: number | string
  ratingCount: number
  approvedByUserId?: BackendId | null
  approvedAt?: BackendTimestamp | null
  rejectionReason?: string | null
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type TeacherProfileCDO = BackendCDO<{
  displayName: string
  headline?: string | null
  bio: string
  avatarFileId?: BackendId | null
}>

export type TeacherProfileUDO = BackendUDO<Partial<TeacherProfileCDO>>

export type TeacherProfilePublicRDO = BackendRDO<{
  id: BackendId
  displayName: string
  headline?: string | null
  bio?: string | null
  avatarFileId?: BackendId | null
  ratingAverage: number | string
  ratingCount: number
  approvedAt?: BackendTimestamp | null
}>

export type TestType =
  | 'listening'
  | 'reading'
  | 'writing_task_1'
  | 'writing_task_2'
  | 'speaking_part_1'
  | 'speaking_part_2'
  | 'speaking_part_3'
  | 'full_mock'

export type TestVisibilityStatus =
  | 'draft'
  | 'submitted'
  | 'published'
  | 'rejected'
  | 'archived'

export type TestPriceFilter = 'free' | 'paid'

export type TestCatalogFilters = {
  type?: TestType
  difficulty?: string
  bandMin?: number | string
  bandMax?: number | string
  tag?: string | string[]
  tags?: string | string[]
  category?: string | string[]
  categories?: string | string[]
  language?: string
  price?: TestPriceFilter
  teacherId?: BackendId
}

export type TestCatalogRequest = ListRequest<TestCatalogFilters>

export type RichTextDocument = {
  version: 1
  type: 'doc'
  content: Array<Record<string, unknown>>
}

export type RichTextContentRDO = BackendRDO<{
  contentDocument?: RichTextDocument | Record<string, unknown> | null
  contentText?: string | null
  renderedHtml?: string | null
}>

export type TeacherTestCDO = BackendCDO<{
  title?: string | null
  description?: string | null
  type?: TestType | string | null
  difficulty?: string | null
  estimatedBandMin?: number | string | null
  estimatedBandMax?: number | string | null
  durationMinutes?: number | null
  priceAmount?: number | string | null
  currency?: string | null
  free?: boolean | null
  language?: string | null
}>

export type TeacherTestUDO = BackendUDO<TeacherTestCDO>

export type TeacherTestListFilters = {
  status?: TestVisibilityStatus | string
}

export type TeacherTestListRequest = ListRequest<TeacherTestListFilters>

export type TeacherTestSectionCDO = BackendCDO<{
  title?: string | null
  instructions?: string | null
  instructionsDocument?: RichTextDocument | Record<string, unknown> | null
  passageText?: string | null
  passageDocument?: RichTextDocument | Record<string, unknown> | null
  audioFileId?: BackendId | null
  durationMinutes?: number | null
}>

export type TeacherTestSectionUDO = BackendUDO<TeacherTestSectionCDO>

export type TeacherTestSectionReorderUDO = BackendUDO<{
  sectionIds: BackendId[]
}>

export type TeacherReadingParagraphCDO = BackendCDO<{
  label?: string | null
  content?: string | null
  contentDocument?: RichTextDocument | Record<string, unknown> | null
}>

export type TeacherReadingParagraphUDO = BackendUDO<TeacherReadingParagraphCDO>

export type TeacherReadingParagraphReorderUDO = BackendUDO<{
  paragraphIds: BackendId[]
}>

export type TeacherReadingParagraphRDO = BackendRDO<{
  id: BackendId
  label?: string | null
  content?: string | null
  contentDocument?: RichTextDocument | Record<string, unknown> | null
  contentText?: string | null
  renderedHtml?: string | null
  paragraphOrder: number
}>

export type TeacherListeningSegmentCDO = BackendCDO<{
  title?: string | null
  startSeconds: number
  endSeconds: number
  transcript?: string | null
}>

export type TeacherListeningSegmentUDO = BackendUDO<{
  title?: string | null
  startSeconds?: number | null
  endSeconds?: number | null
  transcript?: string | null
}>

export type TeacherListeningSegmentReorderUDO = BackendUDO<{
  segmentIds: BackendId[]
}>

export type TeacherListeningSegmentRDO = BackendRDO<{
  id: BackendId
  title: string
  startSeconds?: number | null
  endSeconds?: number | null
  transcript?: string | null
  segmentOrder: number
}>

export type StudentListeningSegmentRDO = TeacherListeningSegmentRDO

export type TeacherQuestionOptionCDO = BackendCDO<{
  optionLabel: string
  optionText?: string | null
  optionDocument?: RichTextDocument | Record<string, unknown> | null
}>

export type TeacherQuestionOptionRDO = BackendRDO<{
  id: BackendId
  optionLabel?: string | null
  optionText: string
  optionTextContent?: RichTextContentRDO | null
  optionOrder: number
}>

export type TeacherQuestionAnswerCDO = BackendCDO<{
  answerValue?: string | null
  primary?: boolean | null
  explanation?: string | null
  explanationDocument?: RichTextDocument | Record<string, unknown> | null
}>

export type TeacherQuestionAnswerRDO = BackendRDO<{
  id: BackendId
  answerValue: string
  normalizedValue?: string | null
  primary: boolean
  answerOrder: number
  explanation?: string | null
  explanationContent?: RichTextContentRDO | null
}>

export type TeacherQuestionCDO = BackendCDO<{
  questionType: string
  questionText?: string | null
  questionDocument?: RichTextDocument | Record<string, unknown> | null
  instructions?: string | null
  instructionsDocument?: RichTextDocument | Record<string, unknown> | null
  score?: number | string | null
  relatedParagraphLabel?: string | null
  listeningSegmentId?: BackendId | null
  options?: TeacherQuestionOptionCDO[] | null
  answers?: TeacherQuestionAnswerCDO[] | null
}>

export type TeacherQuestionUDO = BackendUDO<Partial<TeacherQuestionCDO>>

export type TeacherQuestionRDO = BackendRDO<{
  id: BackendId
  questionType: string
  questionText: string
  questionTextContent?: RichTextContentRDO | null
  instructions?: string | null
  instructionsContent?: RichTextContentRDO | null
  score?: number | string | null
  questionOrder: number
  relatedParagraphLabel?: string | null
  listeningSegmentId?: BackendId | null
  options?: TeacherQuestionOptionRDO[]
  answerKeys?: TeacherQuestionAnswerRDO[]
}>

export type TestDetailSectionRDO = BackendRDO<{
  id: BackendId
  title: string
  instructions?: string | null
  instructionsContent?: RichTextContentRDO | null
  passageText?: string | null
  passageTextContent?: RichTextContentRDO | null
  audioFileId?: BackendId | null
  sectionOrder: number
  durationMinutes?: number | null
  paragraphs?: TeacherReadingParagraphRDO[]
  listeningSegments?: StudentListeningSegmentRDO[]
  questions?: StudentQuestionRDO[]
}>

export type TeacherTestSectionRDO = BackendRDO<
  Omit<TestDetailSectionRDO, 'listeningSegments' | 'questions'> & {
    listeningSegments?: TeacherListeningSegmentRDO[]
    questions?: TeacherQuestionRDO[]
    createdAt?: BackendTimestamp | null
    updatedAt?: BackendTimestamp | null
  }
>

export type TestCardRDO = BackendRDO<{
  id: BackendId
  title: string
  description?: string | null
  type: TestType
  difficulty?: string | null
  estimatedBandMin?: number | string | null
  estimatedBandMax?: number | string | null
  durationMinutes: number
  priceAmount: number | string
  currency: string
  free: boolean
  language: string
  publishedAt?: BackendTimestamp | null
  teacher?: TestTeacherRDO | null
  categories: TestCategoryRDO[]
  tags: TestTagRDO[]
}>

export type TestDetailRDO = BackendRDO<
  TestCardRDO & {
    sections: TestDetailSectionRDO[]
  }
>

export type TeacherTestRDO = BackendRDO<{
  id: BackendId
  teacherProfileId: BackendId
  createdByUserId: BackendId
  title: string
  description?: string | null
  type: TestType | string
  difficulty?: string | null
  estimatedBandMin?: number | string | null
  estimatedBandMax?: number | string | null
  durationMinutes?: number | null
  priceAmount?: number | string | null
  currency?: string | null
  free?: boolean | null
  language?: string | null
  visibility: TestVisibilityStatus | string
  moderationNote?: string | null
  publishedAt?: BackendTimestamp | null
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
  sections: TeacherTestSectionRDO[]
}>

export type TeacherTestSummaryRDO = BackendRDO<Omit<TeacherTestRDO, 'sections'>>

export type StudentQuestionOptionRDO = BackendRDO<{
  id: BackendId
  optionLabel?: string | null
  optionText: string
  optionTextContent?: RichTextContentRDO | null
  optionOrder: number
}>

export type StudentQuestionRDO = BackendRDO<{
  id: BackendId
  questionType: string
  questionText: string
  questionTextContent?: RichTextContentRDO | null
  instructions?: string | null
  instructionsContent?: RichTextContentRDO | null
  questionOrder: number
  relatedParagraphLabel?: string | null
  listeningSegmentId?: BackendId | null
  options?: StudentQuestionOptionRDO[]
}>

export type StudentAnswerUDO = BackendUDO<{
  questionId: BackendId
  answerText?: string | null
  selectedOptionId?: BackendId | null
  audioFileId?: BackendId | null
}>

export type StudentAnswerRDO = BackendRDO<{
  id: BackendId
  attemptId: BackendId
  questionId: BackendId
  answerText?: string | null
  selectedOptionId?: BackendId | null
  audioFileId?: BackendId | null
  answeredAt: BackendTimestamp
}>

export type StudentTestAttemptRDO = BackendRDO<{
  id: BackendId
  testId: BackendId
  studentUserId: BackendId
  status: 'in_progress' | 'submitted' | 'expired' | 'scored'
  startedAt: BackendTimestamp
  submittedAt?: BackendTimestamp | null
  expiresAt?: BackendTimestamp | null
  durationMinutes: number
  remainingSeconds: number
  timeSpentSeconds: number
}>

export type ResultSectionRDO = BackendRDO<{
  id: BackendId
  title: string
  sectionOrder: number
  rawScore?: number | string | null
  maxScore?: number | string | null
  correctCount: number
  incorrectCount: number
}>

export type ResultAnswerReviewRDO = BackendRDO<{
  questionId: BackendId
  sectionId: BackendId
  sectionTitle: string
  questionOrder: number
  questionType: string
  questionText: string
  questionRenderedHtml?: string | null
  studentAnswer?: string | null
  selectedOptionId?: BackendId | null
  selectedOptionLabel?: string | null
  correctAnswers: string[]
  correct: boolean
  scoreAwarded?: number | string | null
  maxScore?: number | string | null
  unanswered: boolean
  explanation?: string | null
  explanationContent?: RichTextContentRDO | null
}>

export type ResultWeakAreaRDO = BackendRDO<{
  questionType: string
  sectionId: BackendId
  sectionTitle: string
  incorrectCount: number
  totalCount: number
}>

export type ResultDetailRDO = BackendRDO<{
  id: BackendId
  attemptId: BackendId
  testId: BackendId
  testTitle: string
  testType: string
  rawScore?: number | string | null
  maxScore?: number | string | null
  estimatedBand?: number | string | null
  correctCount: number
  incorrectCount: number
  startedAt: BackendTimestamp
  submittedAt?: BackendTimestamp | null
  timeSpentSeconds: number
  sections: ResultSectionRDO[]
  answers: ResultAnswerReviewRDO[]
  weakAreas: ResultWeakAreaRDO[]
}>

export type ProgressSummaryRDO = BackendRDO<{
  overallBandEstimate?: number | string | null
  skillBands: Partial<Record<StudyPlanSkill, number | string | null>>
  missingSkills: StudyPlanSkill[]
  completedAttemptCount: number
  latestResultAt?: BackendTimestamp | null
}>

export type SkillScorePointRDO = BackendRDO<{
  resultId: BackendId
  attemptId: BackendId
  testId: BackendId
  testTitle: string
  attemptStatus: string
  scoredAt: BackendTimestamp
  rawScore?: number | string | null
  maxScore?: number | string | null
  estimatedBand?: number | string | null
}>

export type WeakQuestionTypeRDO = BackendRDO<{
  skill: StudyPlanSkill | string
  questionType: string
  incorrectCount: number
  attemptCount: number
  latestSeenAt?: BackendTimestamp | null
}>

export type RecentAttemptRDO = BackendRDO<{
  attemptId: BackendId
  resultId?: BackendId | null
  testId: BackendId
  testTitle: string
  testType: TestType | string
  attemptStatus: string
  startedAt: BackendTimestamp
  submittedAt?: BackendTimestamp | null
  timeSpentSeconds?: number | null
  rawScore?: number | string | null
  maxScore?: number | string | null
  estimatedBand?: number | string | null
}>

export type ProgressRecommendationRDO = BackendRDO<{
  sourceType: string
  testId?: BackendId | null
  title: string
  reason: string
  testType?: TestType | string | null
  directlyAvailable: boolean
  free: boolean
  publishedAt?: BackendTimestamp | null
}>

export type ProgressExtensionHookRDO = BackendRDO<{
  skill: StudyPlanSkill | string
  source: string
  status: string
}>

export type StudentProgressDashboardRDO = BackendRDO<{
  state: 'empty' | 'active' | string
  summary: ProgressSummaryRDO
  skillHistories: Partial<Record<StudyPlanSkill | string, SkillScorePointRDO[]>>
  weakAreas: WeakQuestionTypeRDO[]
  recentAttempts: RecentAttemptRDO[]
  recommendations: ProgressRecommendationRDO[]
  aiUsage?: AiUsageRemainingRDO | null
  extensionHooks: ProgressExtensionHookRDO[]
}>

export type AiUsageRemainingRDO = BackendRDO<{
  periodStart: BackendDate
  periodEnd: BackendDate
  freeRequestsLimit: number
  usedRequests: number
  hardLimitRequests?: number | null
  freeRequestsRemaining: number
  hardRequestsRemaining?: number | null
  hardLimitReached: boolean
}>

export type SpeakingPartType =
  | 'speaking_part_1'
  | 'speaking_part_2'
  | 'speaking_part_3'

export type TranscriptionStatus =
  | 'queued'
  | 'processing'
  | 'completed'
  | 'failed'

export type SpeakingSubmissionCDO = BackendCDO<{
  partType: SpeakingPartType
  prompt: string
  audioFileId: BackendId
  testAttemptId?: BackendId | null
  durationSeconds: number
}>

export type SpeakingSubmissionRDO = BackendRDO<{
  id: BackendId
  studentUserId: BackendId
  testAttemptId: BackendId | null
  partType: SpeakingPartType
  prompt: string
  audioFileId: BackendId
  transcript: string | null
  durationSeconds: number
  transcriptionStatus: TranscriptionStatus
  provider: string | null
  model: string | null
  failureCode: string | null
  errorMessage: string | null
  estimatedCost: string | number
  transcriptionStartedAt: BackendTimestamp | null
  transcriptionCompletedAt: BackendTimestamp | null
  createdAt: BackendTimestamp
}>

export type AiFeedbackStatus = 'queued' | 'processing' | 'completed' | 'failed'

export type WritingTaskType = 'writing_task_1' | 'writing_task_2'

export type WritingSubmissionCDO = BackendCDO<{
  taskType: WritingTaskType
  prompt: string
  answerText: string
  testAttemptId?: BackendId | null
}>

export type WritingSubmissionRDO = BackendRDO<{
  id: BackendId
  studentUserId: BackendId
  testAttemptId?: BackendId | null
  taskType: WritingTaskType
  prompt: string
  answerText: string
  wordCount: number
  feedback?: WritingFeedbackRDO | null
  createdAt: BackendTimestamp
}>

export type WritingCriterionFeedbackRDO = BackendRDO<{
  band?: number | string | null
  feedback: string
  suggestions: string[]
}>

export type WritingMistakeRDO = BackendRDO<{
  type: string
  original: string
  correction?: string | null
  explanation?: string | null
}>

export type WritingFeedbackRDO = BackendRDO<{
  id: BackendId
  submissionId: BackendId
  feedbackType: string
  status: AiFeedbackStatus
  overallBand?: number | string | null
  criteriaScores: Record<string, WritingCriterionFeedbackRDO>
  mistakes: WritingMistakeRDO[]
  improvedAnswer?: string | null
  actionPlan: string[]
  failureCode?: string | null
  failureMessage?: string | null
  provider?: string | null
  model?: string | null
  promptTokens: number
  completionTokens: number
  estimatedCost: number | string
  rubricVersion?: string | null
  retryCount: number
  retryOfFeedbackId?: BackendId | null
  createdAt: BackendTimestamp
  completedAt?: BackendTimestamp | null
}>

export type SpeakingCriterionFeedbackRDO = BackendRDO<{
  band?: number | string | null
  feedback: string
  suggestions: string[]
}>

export type SpeakingMistakeRDO = BackendRDO<{
  type: string
  original: string
  correction?: string | null
  explanation?: string | null
}>

export type SpeakingFeedbackRDO = BackendRDO<{
  id: BackendId
  submissionId: BackendId
  feedbackType: string
  status: AiFeedbackStatus
  overallBand?: number | string | null
  criteriaScores: Record<string, SpeakingCriterionFeedbackRDO>
  repeatedMistakes: SpeakingMistakeRDO[]
  betterAnswerExample?: string | null
  actionPlan: string[]
  transcriptOnlyPronunciation: boolean
  pronunciationScoringNote: string
  failureCode?: string | null
  failureMessage?: string | null
  provider?: string | null
  model?: string | null
  promptTokens: number
  completionTokens: number
  estimatedCost: number | string
  rubricVersion?: string | null
  retryCount: number
  retryOfFeedbackId?: BackendId | null
  createdAt: BackendTimestamp
  completedAt?: BackendTimestamp | null
}>

export type StudyPlanResourceType =
  | 'test'
  | 'listing'
  | 'diagnostic'
  | 'free_practice'

export type StudyPlanSkill =
  | 'listening'
  | 'reading'
  | 'writing'
  | 'speaking'

export type StudyPlanResourceRDO = BackendRDO<{
  type: StudyPlanResourceType
  id?: BackendId | null
  title?: string | null
}>

export type StudyPlanTaskRDO = BackendRDO<{
  id: BackendId
  taskDate: BackendDate
  taskOrder: number
  skill: StudyPlanSkill
  title: string
  description: string
  estimatedMinutes: number
  recommendedResource: StudyPlanResourceRDO
  rationale: string
  completed: boolean
  completedAt?: BackendTimestamp | null
}>

export type StudyPlanCDO = BackendCDO<{
  targetBand: number | string
  durationDays: number
  availableMinutesPerDay?: number | null
  startsAt?: BackendDate | null
}>

export type StudyPlanRDO = BackendRDO<{
  id: BackendId
  studentUserId: BackendId
  targetBand: number | string
  currentBand?: number | string | null
  durationDays: number
  weakestSkills: string[]
  planSummary: Record<string, unknown>
  startsAt: BackendDate
  endsAt: BackendDate
  createdByFeedbackId: BackendId
  tasks: StudyPlanTaskRDO[]
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type TestTeacherRDO = BackendRDO<{
  id: BackendId
  displayName: string
  headline?: string | null
  ratingAverage: number | string
  ratingCount: number
}>

export type TestCategoryRDO = BackendRDO<{
  id: BackendId
  parentId?: BackendId | null
  name: string
  slug: string
  sortOrder: number
}>

export type TestTagRDO = BackendRDO<{
  id: BackendId
  name: string
  slug: string
}>

export type MarketplaceListingType =
  | 'single_test'
  | 'test_pack'
  | 'writing_course'
  | 'speaking_question_pack'
  | 'vocabulary_pack'
  | 'full_mock_exam'
  | 'subscription_plan'

export type MarketplaceListingFilters = {
  type?: MarketplaceListingType
  teacherId?: BackendId
  tag?: string | string[]
  tags?: string | string[]
  category?: string | string[]
  categories?: string | string[]
  language?: string
  price?: TestPriceFilter
  minPrice?: number | string
  maxPrice?: number | string
}

export type MarketplaceListingRequest = ListRequest<MarketplaceListingFilters>

export type MarketplaceListingCDO = BackendCDO<{
  title: string
  description?: string | null
  listingType: MarketplaceListingType
  testId?: BackendId | null
  priceAmount: number | string
  currency: string
  language: string
}>

export type MarketplaceListingUDO = BackendUDO<{
  title?: string | null
  description?: string | null
  listingType?: MarketplaceListingType | null
  testId?: BackendId | null
  priceAmount?: number | string | null
  currency?: string | null
  language?: string | null
}>

export type MarketplaceListingItemCDO = BackendCDO<{
  testId?: BackendId | null
  fileId?: BackendId | null
  title?: string | null
  description?: string | null
  itemOrder: number
}>

export type MarketplaceListingItemUDO = BackendUDO<{
  testId?: BackendId | null
  fileId?: BackendId | null
  title?: string | null
  description?: string | null
  itemOrder?: number | null
}>

export type MarketplaceListingItemRDO = BackendRDO<{
  id: BackendId
  testId?: BackendId | null
  fileId?: BackendId | null
  title?: string | null
  description?: string | null
  itemOrder: number
}>

export type AccessStatusRDO = BackendRDO<{
  listingId: BackendId
  accessible: boolean
  status: 'accessible' | 'payment_required' | 'expired' | 'not_accessible'
  reasonCode?: ApiErrorCode | null
  purchaseId?: BackendId | null
  accessExpiresAt?: BackendTimestamp | null
}>

export type MarketplaceListingSummaryRDO = BackendRDO<{
  id: BackendId
  title: string
  description?: string | null
  listingType: MarketplaceListingType
  testId?: BackendId | null
  priceAmount: number | string
  currency: string
  free: boolean
  active: boolean
  language: string
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
  teacher: TestTeacherRDO
  categories: TestCategoryRDO[]
  tags: TestTagRDO[]
  itemCount: number
  accessStatus?: AccessStatusRDO | null
}>

export type MarketplaceListingDetailRDO = BackendRDO<
  Omit<MarketplaceListingSummaryRDO, 'itemCount'> & {
    items: MarketplaceListingItemRDO[]
  }
>

export type MarketplaceListingActivationRDO = BackendRDO<{
  listingId: BackendId
  ready: boolean
  errors: string[]
}>

export type PurchaseRDO = BackendRDO<{
  id: BackendId
  studentUserId: BackendId
  listingId: BackendId
  listingTitle: string
  listingType: MarketplaceListingType
  amount: number | string
  currency: string
  status: 'granted' | 'revoked' | 'refunded'
  sourceType:
    | 'free_listing'
    | 'payment'
    | 'manual_admin'
    | 'subscription'
    | 'legacy'
  sourceReference?: string | null
  paymentId?: BackendId | null
  paymentStatus?: 'pending' | 'paid' | 'failed' | 'cancelled' | 'refunded' | null
  purchasedAt: BackendTimestamp
  accessExpiresAt?: BackendTimestamp | null
  accessStatus: AccessStatusRDO
}>

export type TeacherWalletRDO = BackendRDO<{
  id: BackendId
  teacherProfileId: BackendId
  balanceAmount: number | string
  pendingAmount: number | string
  availableAmount: number | string
  currency: string
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type TeacherWalletTransactionType =
  | 'earning'
  | 'commission'
  | 'withdrawal'
  | 'refund'
  | 'adjustment'

export type TeacherWalletTransactionSourceType =
  | 'marketplace_purchase'
  | 'live_speaking_booking'
  | 'withdrawal_request'
  | 'refund'
  | 'adjustment'

export type TeacherWalletTransactionRDO = BackendRDO<{
  id: BackendId
  walletId: BackendId
  teacherProfileId: BackendId
  purchaseId?: BackendId | null
  paymentId?: BackendId | null
  withdrawalRequestId?: BackendId | null
  transactionType: TeacherWalletTransactionType
  sourceType?: TeacherWalletTransactionSourceType | null
  sourceReference?: string | null
  grossAmount: number | string
  platformCommissionAmount: number | string
  netAmount: number | string
  currency: string
  description?: string | null
  createdAt: BackendTimestamp
}>

export type WithdrawalRequestStatus =
  | 'requested'
  | 'approved'
  | 'rejected'
  | 'paid'
  | 'cancelled'

export type WithdrawalPayoutMethod = 'card' | 'bank_transfer'

export type WithdrawalCardPayoutDetails = BackendCDO<{
  method: 'card'
  cardNumber: string
  cardHolder: string
}>

export type WithdrawalBankTransferPayoutDetails = BackendCDO<{
  method: 'bank_transfer'
  bankName: string
  accountNumber: string
  accountHolder: string
}>

export type WithdrawalPayoutDetails =
  | WithdrawalCardPayoutDetails
  | WithdrawalBankTransferPayoutDetails

export type WithdrawalRequestCDO = BackendCDO<{
  amount: number
  currency?: string
  payoutDetails: WithdrawalPayoutDetails
}>

export type WithdrawalRequestRDO = BackendRDO<{
  id: BackendId
  teacherProfileId: BackendId
  requestedAmount: number | string
  currency: string
  status: WithdrawalRequestStatus
  payoutDetails: Partial<WithdrawalPayoutDetails> & { method: WithdrawalPayoutMethod | string }
  reviewedByUserId?: BackendId | null
  reviewedAt?: BackendTimestamp | null
  rejectionReason?: string | null
  paidAt?: BackendTimestamp | null
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type TeacherWithdrawalListRequest = PageRequest & {
  status?: WithdrawalRequestStatus
}

export type TeacherDashboardProfileRDO = BackendRDO<{
  id?: BackendId | null
  displayName?: string | null
  status: TeacherProfileStatus
  profileComplete?: boolean | null
  rejectionReason?: string | null
  submittedAt?: BackendTimestamp | null
  approvedAt?: BackendTimestamp | null
  updatedAt?: BackendTimestamp | null
}>

export type TeacherDashboardTestCountsRDO = BackendRDO<{
  draft: number
  submitted: number
  pendingReview?: number
  published: number
  rejected: number
}>

export type TeacherDashboardSalesRDO = BackendRDO<{
  recentSalesCount: number
  recentSalesAmount: number | string
  totalSalesAmount?: number | string | null
  currency: string
  latestSaleAt?: BackendTimestamp | null
}>

export type TeacherDashboardAttemptsRDO = BackendRDO<{
  recentAttemptCount: number
  latestAttemptAt?: BackendTimestamp | null
}>

export type TeacherDashboardReviewsRDO = BackendRDO<{
  averageRating?: number | string | null
  ratingCount: number
  recentReviewCount: number
}>

export type TeacherReviewSummaryRDO = BackendRDO<{
  ratingAverage?: number | string | null
  ratingCount: number
}>

export type TeacherReviewRDO = BackendRDO<{
  id: BackendId
  testId: BackendId
  testTitle: string
  rating: number
  comment?: string | null
  studentDisplayName?: string | null
  createdAt: BackendTimestamp
}>

export type TeacherDashboardNotificationsRDO = BackendRDO<{
  unreadCount: number
  latestNotificationAt?: BackendTimestamp | null
}>

export type TeacherDashboardScheduleSessionRDO = BackendRDO<{
  bookingId: BackendId
  startsAt: BackendTimestamp
  endsAt?: BackendTimestamp | null
  status: LiveSpeakingBookingStatus | string
  studentDisplayName?: string | null
  meetUrl?: string | null
  joinLinkAvailable?: boolean | null
}>

export type TeacherDashboardScheduleRDO = BackendRDO<{
  enabled: boolean
  nextSession?: TeacherDashboardScheduleSessionRDO | null
  pendingMeetLinkCount?: number
}>

export type TeacherDashboardRDO = BackendRDO<{
  state?: 'empty' | 'active' | string
  profile: TeacherDashboardProfileRDO
  tests?: TeacherDashboardTestCountsRDO | null
  wallet?: TeacherWalletRDO | null
  sales?: TeacherDashboardSalesRDO | null
  attempts?: TeacherDashboardAttemptsRDO | null
  reviews?: TeacherDashboardReviewsRDO | null
  notifications?: TeacherDashboardNotificationsRDO | null
  schedule?: TeacherDashboardScheduleRDO | null
}>

export type PaymentStatus =
  | 'pending'
  | 'paid'
  | 'failed'
  | 'cancelled'
  | 'refunded'

export type PaymentTargetType =
  | 'marketplace_listing'
  | 'subscription'
  | 'live_speaking_booking'
  | 'legacy'

export type PaymentCDO = BackendCDO<{
  targetType: PaymentTargetType
  listingId?: BackendId | null
  subscriptionId?: BackendId | null
  liveSpeakingBookingId?: BackendId | null
  promoCode?: string | null
  returnUrl?: string | null
}>

export type PaymentRDO = BackendRDO<{
  id: BackendId
  studentUserId: BackendId
  provider: string
  providerPaymentId?: string | null
  providerCheckoutUrl?: string | null
  status: PaymentStatus
  amount: number | string
  currency: string
  targetType: PaymentTargetType
  targetListingId?: BackendId | null
  targetListingTitle?: string | null
  targetSubscriptionId?: BackendId | null
  targetLiveSpeakingBookingId?: BackendId | null
  promoCode?: string | null
  promoOriginalAmount?: number | string | null
  promoDiscountAmount?: number | string | null
  providerAmount?: number | string | null
  providerCurrency?: string | null
  failureCode?: string | null
  failureMessage?: string | null
  createdAt: BackendTimestamp
  paidAt?: BackendTimestamp | null
  confirmationDispatchedAt?: BackendTimestamp | null
  updatedAt: BackendTimestamp
}>

export type AdminPaymentListRequest = PageRequest & {
  status?: PaymentStatus
}

export type AdminPaymentRDO = PaymentRDO

export type PaymentDisputeStatus =
  | 'open'
  | 'under_review'
  | 'refunded'
  | 'rejected'
  | 'resolved'

export type AdminPaymentDisputeListRequest = PageRequest & {
  status?: PaymentDisputeStatus
  paymentStatus?: PaymentStatus
  teacherProfileId?: BackendId
  studentUserId?: BackendId
  reason?: string
  from?: BackendTimestamp
  to?: BackendTimestamp
}

export type AdminPaymentDisputeRDO = BackendRDO<{
  id: BackendId
  paymentId: BackendId
  paymentStatus: PaymentStatus | string
  paymentAmount: number | string
  paymentCurrency: string
  paymentTargetType: PaymentTargetType | string
  paymentTargetListingId?: BackendId | null
  paymentTargetLiveSpeakingBookingId?: BackendId | null
  studentUserId: BackendId
  studentDisplayName?: string | null
  teacherProfileId?: BackendId | null
  teacherDisplayName?: string | null
  status: PaymentDisputeStatus | string
  reason: string
  description?: string | null
  resolutionNote?: string | null
  reviewedByUserId?: BackendId | null
  reviewedAt?: BackendTimestamp | null
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type PaymentDisputeDecisionCDO = BackendCDO<{
  status: Exclude<PaymentDisputeStatus, 'open'>
  resolutionNote?: string | null
}>

export type AdminWithdrawalRequestRDO = WithdrawalRequestRDO & {
  teacherUserId: BackendId
  teacherDisplayName: string
}

export type AdminWithdrawalListRequest = PageRequest & {
  status?: WithdrawalRequestStatus
  teacherProfileId?: BackendId
}

export type WithdrawalRejectionCDO = BackendCDO<{
  reason: string
}>

export type AdminModerationTestListRequest = PageRequest & {
  type?: TestType | string
  teacherProfileId?: BackendId
  difficulty?: string
  submittedFrom?: BackendTimestamp
  submittedTo?: BackendTimestamp
  search?: string
}

export type AdminModerationTestRDO = BackendRDO<{
  id: BackendId
  teacherProfileId: BackendId
  teacherDisplayName: string
  title: string
  type: TestType | string
  difficulty?: string | null
  durationMinutes?: number | null
  priceAmount?: number | string | null
  currency: string
  free: boolean
  visibility: TestVisibilityStatus | string
  submittedAt?: BackendTimestamp | null
  createdAt: BackendTimestamp
}>

export type AdminModerationQueueMetaRDO = BackendRDO<{
  pendingCount: number
  oldestSubmittedAt?: BackendTimestamp | null
}>

export type AdminModerationTestDetailRDO = BackendRDO<{
  test: TeacherTestRDO
  readyForDecision: boolean
  validationFindings: Record<string, string>
}>

export type TestModerationRejectCDO = BackendCDO<{
  reason: string
}>

export type AdminReportName =
  | 'ai-usage'
  | 'sales'
  | 'revenue'
  | 'test-activity'
  | 'provider-failures'

export type AdminReportRequest = DateRangeFilter & {
  studentUserId?: BackendId
  teacherProfileId?: BackendId
  listingId?: BackendId
  feedbackType?: string
  status?: string
  provider?: string
  model?: string
  paymentStatus?: PaymentStatus | string
  testType?: TestType | string
}

export type AdminReportRDO = BackendRDO<{
  generatedAt: BackendTimestamp
  filters: Record<string, unknown>
  totals: Record<string, number | string | null>
  rows: Array<Record<string, number | string | null>>
  liveSpeakingStatus?: string | null
}>

export type AdminDashboardStatusCountRDO = BackendRDO<{
  status: string
  count: number
}>

export type AdminDashboardUsersRDO = BackendRDO<{
  total: number
  active: number
  suspended: number
  banned: number
  deleted: number
  newInWindow: number
}>

export type AdminDashboardTeachersRDO = BackendRDO<{
  total: number
  draft: number
  pendingApproval: number
  approved: number
  rejected: number
  suspended: number
}>

export type AdminDashboardTestsRDO = BackendRDO<{
  total: number
  draft: number
  submitted: number
  published: number
  rejected: number
  archived: number
}>

export type AdminDashboardPaymentsRDO = BackendRDO<{
  total: number
  pending: number
  paid: number
  failed: number
  cancelled: number
  refunded: number
  amountMismatch: number
  paidAmountTotal: number | string
}>

export type AdminDashboardWithdrawalsRDO = BackendRDO<{
  total: number
  requested: number
  approved: number
  rejected: number
  paid: number
  cancelled: number
  requestedAmountTotal: number | string
}>

export type AdminDashboardAiUsageRDO = BackendRDO<{
  totalRequests: number
  completedRequests: number
  failedRequests: number
  validationFailedRequests: number
  blockedRequests: number
  promptTokens: number
  completionTokens: number
  estimatedCost: number | string
}>

export type AdminDashboardQueuesRDO = BackendRDO<{
  pendingTeacherApprovals: number
  pendingTestModeration: number
  pendingWithdrawals: number
  failedPayments: number
  amountMismatchPayments: number
}>

export type AdminDashboardActivityRDO = BackendRDO<{
  type: string
  referenceId: BackendId
  summary: string
  occurredAt: BackendTimestamp
}>

export type AdminDashboardMeta = DashboardMetadata & {
  window?: DateRangeFilter
}

export type AdminDashboardRDO = BackendRDO<{
  users: AdminDashboardUsersRDO
  teachers: AdminDashboardTeachersRDO
  tests: AdminDashboardTestsRDO
  payments: AdminDashboardPaymentsRDO
  withdrawals: AdminDashboardWithdrawalsRDO
  aiUsage: AdminDashboardAiUsageRDO
  queues: AdminDashboardQueuesRDO
  recentActivity: AdminDashboardActivityRDO[]
}>

export type AdminTeacherProfileListRequest = PageRequest & {
  status?: TeacherProfileStatus
}

export type AdminUserRoleFilter =
  | 'student'
  | 'teacher'
  | 'admin'
  | 'school_manager'
  | 'moderator'

export type AdminUserListRequest = ListRequest<{
  role?: AdminUserRoleFilter
  status?: AdminUserStatus
  teacherStatus?: TeacherProfileStatus
}>

export type AdminUserRDO = BackendRDO<{
  id: BackendId
  email: string
  fullName: string
  phone?: string | null
  status: AdminUserStatus | string
  teacherStatus?: TeacherProfileStatus | null
  preferredLanguage?: string | null
  roles: string[]
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
  deletedAt?: BackendTimestamp | null
}>

export type AdminUserStatusUDO = BackendUDO<{
  status: AdminUserStatus
  reason?: string | null
}>

export type TeacherProfileModerationUDO = BackendUDO<{
  rejectionReason: string
}>

export type AdminTeacherProfileRecentTestRDO = BackendRDO<{
  id: BackendId
  title: string
  visibility: TestVisibilityStatus | string
  updatedAt: BackendTimestamp
}>

export type AdminTeacherProfileDetailRDO = BackendRDO<{
  profile: TeacherProfileRDO
  totalTestCount: number
  publishedTestCount: number
  wallet?: TeacherWalletRDO | null
  recentTests: AdminTeacherProfileRecentTestRDO[]
}>

export type PromoQuoteRDO = BackendRDO<{
  listingId: BackendId
  promoCode: string
  valid: boolean
  discountAmount: number | string
  finalAmount: number | string
  currency: string
  reasonCode?: ApiErrorCode | null
}>

export type PlatformSettingValue = Record<string, unknown>

export type PlatformSettingRDO = BackendRDO<{
  key: string
  value: PlatformSettingValue
  updatedByUserId?: BackendId | null
  updatedAt: BackendTimestamp
}>

export type PlatformSettingUDO = BackendUDO<{
  value: PlatformSettingValue
}>

export type FilePurpose =
  | 'listening_audio'
  | 'teacher_avatar'
  | 'passage_image'
  | 'diagram'
  | 'document'
  | 'course_material'
  | 'speaking_audio'
  | 'banner_image'

export type FileRDO = BackendRDO<{
  id: BackendId
  purpose: FilePurpose
  filename: string
  contentType: string
  sizeBytes: number
  createdAt: BackendTimestamp
}>

export type FileDownloadUrlRDO = BackendRDO<{
  fileId: BackendId
  url: string
  expiresAt?: BackendTimestamp | null
  expiresInSeconds: number
}>

export type NotificationRDO = BackendRDO<{
  id: BackendId
  title: string
  body: string
  read: boolean
  targetPath?: string | null
  createdAt: BackendTimestamp
  readAt?: BackendTimestamp | null
}>

export type NotificationUnreadCountRDO = BackendRDO<{
  unreadCount: number
}>

export type LiveSpeakingSlotStatus =
  | 'draft'
  | 'announced'
  | 'reserved'
  | 'booked'
  | 'completed'
  | 'cancelled'

export type LiveSpeakingBookingStatus =
  | 'reserved'
  | 'booked'
  | 'completed'
  | 'cancelled'
  | 'expired'
  | 'no_show'

export type LiveSpeakingProviderSyncStatus =
  | 'pending'
  | 'synced'
  | 'failed'
  | 'disabled'
  | 'not_required'

export type LiveSpeakingSlotCDO = BackendCDO<{
  startsAt: BackendTimestamp
  endsAt: BackendTimestamp
  timezone: string
  priceAmount: number | string
  currency: string
}>

export type LiveSpeakingSlotUDO = BackendUDO<Partial<LiveSpeakingSlotCDO>>

export type LiveSpeakingSlotListRequest = PageRequest &
  DateRangeFilter &
  StatusFilter<LiveSpeakingSlotStatus> & {
    teacherProfileId?: BackendId
    timezone?: string
  }

export type LiveSpeakingBookingListRequest = PageRequest &
  DateRangeFilter &
  StatusFilter<LiveSpeakingBookingStatus> & {
    studentUserId?: BackendId
    teacherProfileId?: BackendId
  }

export type LiveSpeakingProviderFailureListRequest = PageRequest &
  DateRangeFilter & {
    provider?: string
    providerSyncStatus?: LiveSpeakingProviderSyncStatus | string
  }

export type LiveSpeakingSlotRDO = BackendRDO<{
  id: BackendId
  teacherProfileId: BackendId
  teacherDisplayName?: string | null
  teacherHeadline?: string | null
  startsAt: BackendTimestamp
  endsAt: BackendTimestamp
  timezone: string
  priceAmount: number | string
  currency: string
  status: LiveSpeakingSlotStatus
  announced: boolean
  holdExpiresAt?: BackendTimestamp | null
  createdAt?: BackendTimestamp | null
  updatedAt?: BackendTimestamp | null
}>

export type LiveSpeakingBookingRDO = BackendRDO<{
  id: BackendId
  slotId: BackendId
  studentUserId: BackendId
  studentDisplayName?: string | null
  teacherProfileId: BackendId
  teacherDisplayName?: string | null
  status: LiveSpeakingBookingStatus
  startsAt: BackendTimestamp
  endsAt: BackendTimestamp
  timezone?: string | null
  priceAmount: number | string
  currency: string
  paymentId?: BackendId | null
  paymentStatus?: PaymentStatus | string | null
  meetUrl?: string | null
  holdExpiresAt?: BackendTimestamp | null
  providerSyncStatus?: LiveSpeakingProviderSyncStatus | string | null
  providerFailureCode?: string | null
  providerFailureMessage?: string | null
  createdAt: BackendTimestamp
  updatedAt: BackendTimestamp
}>

export type LiveSpeakingProviderFailureRDO = BackendRDO<{
  id: BackendId
  bookingId?: BackendId | null
  slotId?: BackendId | null
  teacherProfileId?: BackendId | null
  teacherDisplayName?: string | null
  studentUserId?: BackendId | null
  studentDisplayName?: string | null
  provider?: string | null
  providerSyncStatus?: LiveSpeakingProviderSyncStatus | string | null
  failureCode?: string | null
  failureMessage?: string | null
  retryCount?: number | null
  startsAt?: BackendTimestamp | null
  createdAt: BackendTimestamp
  updatedAt?: BackendTimestamp | null
}>
