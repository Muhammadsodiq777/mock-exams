import {
  api,
  unwrapDashboardResponse,
  unwrapPageResponse,
  unwrapResource,
  unwrapResourceResponse,
  unwrapUnpagedListResponse,
} from './api'
import type { AxiosProgressEvent } from 'axios'
import { apiRoutes } from './apiRoutes'
import { authService } from './auth.service'
import type {
  AdminDashboardRDO,
  AdminUserListRequest,
  AdminUserRDO,
  AdminUserStatusUDO,
  AdminModerationQueueMetaRDO,
  AdminModerationTestDetailRDO,
  AdminModerationTestListRequest,
  AdminModerationTestRDO,
  AdminPaymentDisputeListRequest,
  AdminPaymentDisputeRDO,
  AdminPaymentListRequest,
  AdminPaymentRDO,
  AdminReportName,
  AdminReportRDO,
  AdminReportRequest,
  AdminTeacherProfileDetailRDO,
  AdminTeacherProfileListRequest,
  AdminWithdrawalListRequest,
  AdminWithdrawalRequestRDO,
  AiUsageRemainingRDO,
  ApiDashboardResponse,
  ApiListResponse,
  ApiResourceResult,
  ApiSuccessEnvelope,
  ApiUnpagedListResponse,
  AccessStatusRDO,
  BackendId,
  CurrentUserRDO,
  FileDownloadUrlRDO,
  FileRDO,
  LiveSpeakingBookingRDO,
  LiveSpeakingBookingListRequest,
  LiveSpeakingProviderFailureRDO,
  LiveSpeakingProviderFailureListRequest,
  LiveSpeakingSlotCDO,
  LiveSpeakingSlotListRequest,
  LiveSpeakingSlotRDO,
  LiveSpeakingSlotUDO,
  MarketplaceListingActivationRDO,
  MarketplaceListingCDO,
  MarketplaceListingDetailRDO,
  MarketplaceListingItemCDO,
  MarketplaceListingItemRDO,
  MarketplaceListingItemUDO,
  MarketplaceListingRequest,
  MarketplaceListingSummaryRDO,
  MarketplaceListingUDO,
  MockExamRDO,
  NotificationRDO,
  NotificationUnreadCountRDO,
  PageRequest,
  PaymentCDO,
  PaymentDisputeDecisionCDO,
  PaymentRDO,
  PlatformSettingRDO,
  PlatformSettingUDO,
  PromoQuoteRDO,
  PurchaseRDO,
  ResultDetailRDO,
  SpeakingFeedbackRDO,
  SpeakingSubmissionCDO,
  SpeakingSubmissionRDO,
  StudentAnswerRDO,
  StudentAnswerUDO,
  StudentProgressDashboardRDO,
  StudentTestAttemptRDO,
  StudyPlanCDO,
  StudyPlanRDO,
  TeacherProfileCDO,
  TeacherProfileModerationUDO,
  TeacherProfilePublicRDO,
  TeacherProfileRDO,
  TeacherProfileUDO,
  TeacherDashboardRDO,
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
  TeacherTestSummaryRDO,
  TeacherTestUDO,
  TestCardRDO,
  TestCatalogRequest,
  TestCategoryRDO,
  TestDetailRDO,
  TestTagRDO,
  TeacherReviewRDO,
  TeacherReviewSummaryRDO,
  TeacherWalletRDO,
  TeacherWalletTransactionRDO,
  TeacherWithdrawalListRequest,
  TestModerationRejectCDO,
  WithdrawalRequestCDO,
  WithdrawalRequestRDO,
  WithdrawalRejectionCDO,
  WritingFeedbackRDO,
  WritingSubmissionCDO,
  WritingSubmissionRDO,
} from '../types/backend'

type QueryParams = Record<string, unknown>

type FileUploadOptions = {
  onUploadProgress?: (event: AxiosProgressEvent) => void
}

const params = <TParams extends QueryParams | undefined>(value?: TParams) => ({
  params: value,
})

const adminReportRoute = (name: AdminReportName) => {
  switch (name) {
    case 'ai-usage':
      return apiRoutes.admin.reports.aiUsage
    case 'sales':
      return apiRoutes.admin.reports.sales
    case 'revenue':
      return apiRoutes.admin.reports.revenue
    case 'test-activity':
      return apiRoutes.admin.reports.testActivity
    case 'provider-failures':
      return apiRoutes.admin.reports.providerFailures
  }
}

export { apiRoutes }

export const catalogService = {
  async listTests(filters?: TestCatalogRequest) {
    const response = await api.get<ApiListResponse<TestCardRDO>>(
      apiRoutes.public.tests,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getTest(testId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<TestDetailRDO>>(apiRoutes.public.test(testId))
    return unwrapResource(response)
  },

  async listCategories() {
    const response = await api.get<ApiUnpagedListResponse<TestCategoryRDO>>(
      apiRoutes.public.testCategories,
    )
    return unwrapUnpagedListResponse(response)
  },

  async listTags() {
    const response = await api.get<ApiUnpagedListResponse<TestTagRDO>>(apiRoutes.public.testTags)
    return unwrapUnpagedListResponse(response)
  },

  async listTeacherProfiles(filters?: PageRequest) {
    const response = await api.get<ApiListResponse<TeacherProfilePublicRDO>>(
      apiRoutes.public.teacherProfiles,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getTeacherProfile(profileId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<TeacherProfilePublicRDO>>(
      apiRoutes.public.teacherProfile(profileId),
    )
    return unwrapResource(response)
  },
}

export const attemptService = {
  async startAttempt(testId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<StudentTestAttemptRDO>>(
      apiRoutes.student.testAttempts(testId),
    )
    return unwrapResource(response)
  },

  async saveAnswer(attemptId: BackendId, payload: StudentAnswerUDO) {
    const response = await api.patch<ApiSuccessEnvelope<StudentAnswerRDO>>(
      apiRoutes.student.attemptAnswers(attemptId),
      payload,
    )
    return unwrapResource(response)
  },

  async submitAttempt(attemptId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<StudentTestAttemptRDO>>(
      apiRoutes.student.attemptSubmit(attemptId),
    )
    return unwrapResource(response)
  },
}

export const resultService = {
  async getStudentProgress(filters?: { days?: number }) {
    const response = await api.get<ApiDashboardResponse<StudentProgressDashboardRDO>>(
      apiRoutes.student.progress,
      params(filters),
    )
    return unwrapDashboardResponse(response)
  },

  async getAttemptResult(attemptId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<ResultDetailRDO>>(
      apiRoutes.student.attemptResult(attemptId),
    )
    return unwrapResource(response)
  },

  async getResult(resultId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<ResultDetailRDO>>(
      apiRoutes.student.result(resultId),
    )
    return unwrapResource(response)
  },
}

export const aiPracticeService = {
  async createSpeakingSubmission(payload: SpeakingSubmissionCDO) {
    const response = await api.post<ApiSuccessEnvelope<SpeakingSubmissionRDO>>(
      apiRoutes.student.speakingSubmissions,
      payload,
    )
    return unwrapResource(response)
  },

  async getSpeakingSubmission(submissionId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<SpeakingSubmissionRDO>>(
      apiRoutes.student.speakingSubmission(submissionId),
    )
    return unwrapResource(response)
  },

  async startSpeakingTranscription(submissionId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<SpeakingSubmissionRDO>>(
      apiRoutes.student.speakingTranscription(submissionId),
    )
    return unwrapResource(response)
  },

  async startSpeakingFeedback(submissionId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<SpeakingFeedbackRDO>>(
      apiRoutes.student.speakingFeedback(submissionId),
    )
    return unwrapResource(response)
  },

  async getSpeakingFeedback(submissionId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<SpeakingFeedbackRDO>>(
      apiRoutes.student.speakingFeedback(submissionId),
    )
    return unwrapResource(response)
  },

  async retrySpeakingFeedback(submissionId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<SpeakingFeedbackRDO>>(
      apiRoutes.student.speakingFeedbackRetry(submissionId),
    )
    return unwrapResource(response)
  },

  async createWritingSubmission(payload: WritingSubmissionCDO) {
    const response = await api.post<ApiSuccessEnvelope<WritingSubmissionRDO>>(
      apiRoutes.student.writingSubmissions,
      payload,
    )
    return unwrapResource(response)
  },

  async getWritingSubmission(submissionId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<WritingSubmissionRDO>>(
      apiRoutes.student.writingSubmission(submissionId),
    )
    return unwrapResource(response)
  },

  async getWritingFeedback(submissionId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<WritingFeedbackRDO>>(
      apiRoutes.student.writingFeedback(submissionId),
    )
    return unwrapResource(response)
  },

  async retryWritingFeedback(submissionId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<WritingFeedbackRDO>>(
      apiRoutes.student.writingFeedbackRetry(submissionId),
    )
    return unwrapResource(response)
  },

  async getAiUsage() {
    const response = await api.get<ApiSuccessEnvelope<AiUsageRemainingRDO>>(
      apiRoutes.student.aiUsage,
    )
    return unwrapResource(response)
  },

  async createStudyPlan(payload: StudyPlanCDO) {
    const response = await api.post<ApiSuccessEnvelope<StudyPlanRDO>>(
      apiRoutes.student.studyPlans,
      payload,
    )
    return unwrapResource(response)
  },

  async getActiveStudyPlan() {
    const response = await api.get<ApiUnpagedListResponse<StudyPlanRDO>>(
      apiRoutes.student.activeStudyPlan,
    )
    return unwrapUnpagedListResponse(response)
  },
}

export const marketplaceService = {
  async listPublicListings(filters?: MarketplaceListingRequest) {
    const response = await api.get<ApiListResponse<MarketplaceListingSummaryRDO>>(
      apiRoutes.public.marketplace,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getPublicListing(listingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<MarketplaceListingDetailRDO>>(
      apiRoutes.public.marketplaceListing(listingId),
    )
    return unwrapResource(response)
  },

  async listPurchases() {
    const response = await api.get<ApiUnpagedListResponse<PurchaseRDO>>(
      apiRoutes.student.purchases,
    )
    return unwrapUnpagedListResponse(response)
  },

  async getPurchase(purchaseId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<PurchaseRDO>>(
      apiRoutes.student.purchase(purchaseId),
    )
    return unwrapResource(response)
  },

  async listPayments() {
    const response = await api.get<ApiUnpagedListResponse<PaymentRDO>>(
      apiRoutes.student.payments,
    )
    return unwrapUnpagedListResponse(response)
  },

  async getPayment(paymentId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<PaymentRDO>>(
      apiRoutes.student.payment(paymentId),
    )
    return unwrapResource(response)
  },

  async grantFreeAccess(listingId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<PurchaseRDO>>(
      apiRoutes.student.marketplaceFreeAccess(listingId),
    )
    return unwrapResource(response)
  },

  async getListingAccess(listingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AccessStatusRDO>>(
      apiRoutes.student.marketplaceAccess(listingId),
    )
    return unwrapResource(response)
  },

  async listAccessibleItems(listingId: BackendId) {
    const response = await api.get<ApiUnpagedListResponse<MarketplaceListingItemRDO>>(
      apiRoutes.student.marketplaceItems(listingId),
    )
    return unwrapUnpagedListResponse(response)
  },

  async createPayment(payload: PaymentCDO) {
    const response = await api.post<ApiSuccessEnvelope<PaymentRDO>>(
      apiRoutes.student.payments,
      payload,
    )
    return unwrapResource(response)
  },

  async getPromoQuote(paramsValue: { listingId: BackendId; promoCode: string }) {
    const response = await api.get<ApiSuccessEnvelope<PromoQuoteRDO>>(
      apiRoutes.student.promoQuote,
      params(paramsValue),
    )
    return unwrapResource(response)
  },
}

export const teacherService = {
  async getDashboard() {
    const response = await api.get<ApiDashboardResponse<TeacherDashboardRDO>>(
      apiRoutes.teacher.dashboard,
    )
    return unwrapDashboardResponse(response)
  },

  async getWallet() {
    const response = await api.get<ApiSuccessEnvelope<TeacherWalletRDO>>(
      apiRoutes.teacher.wallet,
    )
    return unwrapResource(response)
  },

  async listWalletTransactions(filters?: PageRequest) {
    const response = await api.get<ApiListResponse<TeacherWalletTransactionRDO>>(
      apiRoutes.teacher.walletTransactions,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async createWithdrawal(payload: WithdrawalRequestCDO) {
    const response = await api.post<ApiSuccessEnvelope<WithdrawalRequestRDO>>(
      apiRoutes.teacher.withdrawals,
      payload,
    )
    return unwrapResource(response)
  },

  async listWithdrawals(filters?: TeacherWithdrawalListRequest) {
    const response = await api.get<ApiListResponse<WithdrawalRequestRDO>>(
      apiRoutes.teacher.withdrawals,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getWithdrawal(withdrawalRequestId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<WithdrawalRequestRDO>>(
      apiRoutes.teacher.withdrawal(withdrawalRequestId),
    )
    return unwrapResource(response)
  },

  async cancelWithdrawal(withdrawalRequestId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<WithdrawalRequestRDO>>(
      apiRoutes.teacher.withdrawalCancel(withdrawalRequestId),
    )
    return unwrapResource(response)
  },

  async listReviews(filters?: PageRequest) {
    const response = await api.get<ApiListResponse<TeacherReviewRDO>>(
      apiRoutes.teacher.reviews,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getReviewSummary() {
    const response = await api.get<ApiSuccessEnvelope<TeacherReviewSummaryRDO>>(
      apiRoutes.teacher.reviewSummary,
    )
    return unwrapResource(response)
  },

  async listNotifications(filters?: PageRequest) {
    const response = await api.get<ApiListResponse<NotificationRDO>>(
      apiRoutes.teacher.notifications,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getNotificationUnreadCount() {
    const response = await api.get<ApiSuccessEnvelope<NotificationUnreadCountRDO>>(
      apiRoutes.teacher.notificationUnreadCount,
    )
    return unwrapResource(response)
  },

  async markNotificationRead(notificationId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<NotificationRDO>>(
      apiRoutes.teacher.notificationRead(notificationId),
    )
    return unwrapResource(response)
  },

  async getProfile() {
    const response = await api.get<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.teacher.profile,
    )
    return unwrapResource(response)
  },

  async createProfile(payload: TeacherProfileCDO) {
    const response = await api.post<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.teacher.profile,
      payload,
    )
    return unwrapResource(response)
  },

  async updateProfile(payload: TeacherProfileUDO) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.teacher.profile,
      payload,
    )
    return unwrapResource(response)
  },

  async submitProfile() {
    const response = await api.post<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.teacher.profileSubmit,
    )
    return unwrapResource(response)
  },

  async listTests(filters?: TeacherTestListRequest) {
    const response = await api.get<ApiListResponse<TeacherTestSummaryRDO>>(
      apiRoutes.teacher.tests,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async createTest(payload: TeacherTestCDO) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.tests,
      payload,
    )
    return unwrapResource(response)
  },

  async updateTest(testId: BackendId, payload: TeacherTestUDO) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.test(testId),
      payload,
    )
    return unwrapResource(response)
  },

  async getTestPreview(testId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testPreview(testId),
    )
    return unwrapResource(response)
  },

  async submitTest(testId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSubmit(testId),
    )
    return unwrapResource(response)
  },

  async archiveTest(testId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testArchive(testId),
    )
    return unwrapResource(response)
  },

  async addTestSection(testId: BackendId, payload: TeacherTestSectionCDO) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSections(testId),
      payload,
    )
    return unwrapResource(response)
  },

  async updateTestSection(
    testId: BackendId,
    sectionId: BackendId,
    payload: TeacherTestSectionUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSection(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async reorderTestSections(testId: BackendId, payload: TeacherTestSectionReorderUDO) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionsReorder(testId),
      payload,
    )
    return unwrapResource(response)
  },

  async deleteTestSection(testId: BackendId, sectionId: BackendId) {
    const response = await api.delete<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSection(testId, sectionId),
    )
    return unwrapResource(response)
  },

  async addReadingParagraph(
    testId: BackendId,
    sectionId: BackendId,
    payload: TeacherReadingParagraphCDO,
  ) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionParagraphs(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async updateReadingParagraph(
    testId: BackendId,
    sectionId: BackendId,
    paragraphId: BackendId,
    payload: TeacherReadingParagraphUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionParagraph(testId, sectionId, paragraphId),
      payload,
    )
    return unwrapResource(response)
  },

  async reorderReadingParagraphs(
    testId: BackendId,
    sectionId: BackendId,
    payload: TeacherReadingParagraphReorderUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionParagraphsReorder(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async deleteReadingParagraph(testId: BackendId, sectionId: BackendId, paragraphId: BackendId) {
    const response = await api.delete<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionParagraph(testId, sectionId, paragraphId),
    )
    return unwrapResource(response)
  },

  async addListeningSegment(
    testId: BackendId,
    sectionId: BackendId,
    payload: TeacherListeningSegmentCDO,
  ) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionListeningSegments(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async updateListeningSegment(
    testId: BackendId,
    sectionId: BackendId,
    segmentId: BackendId,
    payload: TeacherListeningSegmentUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionListeningSegment(testId, sectionId, segmentId),
      payload,
    )
    return unwrapResource(response)
  },

  async reorderListeningSegments(
    testId: BackendId,
    sectionId: BackendId,
    payload: TeacherListeningSegmentReorderUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionListeningSegmentsReorder(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async deleteListeningSegment(testId: BackendId, sectionId: BackendId, segmentId: BackendId) {
    const response = await api.delete<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionListeningSegment(testId, sectionId, segmentId),
    )
    return unwrapResource(response)
  },

  async addQuestion(testId: BackendId, sectionId: BackendId, payload: TeacherQuestionCDO) {
    const response = await api.post<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionQuestions(testId, sectionId),
      payload,
    )
    return unwrapResource(response)
  },

  async updateQuestion(
    testId: BackendId,
    sectionId: BackendId,
    questionId: BackendId,
    payload: TeacherQuestionUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionQuestion(testId, sectionId, questionId),
      payload,
    )
    return unwrapResource(response)
  },

  async deleteQuestion(testId: BackendId, sectionId: BackendId, questionId: BackendId) {
    const response = await api.delete<ApiSuccessEnvelope<TeacherTestRDO>>(
      apiRoutes.teacher.testSectionQuestion(testId, sectionId, questionId),
    )
    return unwrapResource(response)
  },

  async listMarketplaceListings(filters?: PageRequest) {
    const response = await api.get<ApiListResponse<MarketplaceListingSummaryRDO>>(
      apiRoutes.teacher.marketplaceListings,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async createMarketplaceListing(payload: MarketplaceListingCDO) {
    const response = await api.post<ApiSuccessEnvelope<MarketplaceListingDetailRDO>>(
      apiRoutes.teacher.marketplaceListings,
      payload,
    )
    return unwrapResource(response)
  },

  async updateMarketplaceListing(listingId: BackendId, payload: MarketplaceListingUDO) {
    const response = await api.patch<ApiSuccessEnvelope<MarketplaceListingDetailRDO>>(
      apiRoutes.teacher.marketplaceListing(listingId),
      payload,
    )
    return unwrapResource(response)
  },

  async addMarketplaceListingItem(listingId: BackendId, payload: MarketplaceListingItemCDO) {
    const response = await api.post<ApiSuccessEnvelope<MarketplaceListingItemRDO>>(
      apiRoutes.teacher.marketplaceListingItems(listingId),
      payload,
    )
    return unwrapResource(response)
  },

  async updateMarketplaceListingItem(
    listingId: BackendId,
    itemId: BackendId,
    payload: MarketplaceListingItemUDO,
  ) {
    const response = await api.patch<ApiSuccessEnvelope<MarketplaceListingItemRDO>>(
      apiRoutes.teacher.marketplaceListingItem(listingId, itemId),
      payload,
    )
    return unwrapResource(response)
  },

  async getMarketplaceListingActivation(listingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<MarketplaceListingActivationRDO>>(
      apiRoutes.teacher.marketplaceListingActivation(listingId),
    )
    return unwrapResource(response)
  },
}

export const adminService = {
  async getDashboard(filters?: QueryParams) {
    const response = await api.get<ApiDashboardResponse<AdminDashboardRDO>>(
      apiRoutes.admin.dashboard,
      params(filters),
    )
    return unwrapDashboardResponse(response)
  },

  async listUsers(filters?: AdminUserListRequest) {
    const response = await api.get<ApiListResponse<AdminUserRDO>>(
      apiRoutes.admin.users,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getUser(userId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminUserRDO>>(
      apiRoutes.admin.user(userId),
    )
    return unwrapResource(response)
  },

  async updateUserStatus(userId: BackendId, payload: AdminUserStatusUDO) {
    const response = await api.patch<ApiSuccessEnvelope<AdminUserRDO>>(
      apiRoutes.admin.userStatus(userId),
      payload,
    )
    return unwrapResource(response)
  },

  async listTeacherProfiles(filters?: AdminTeacherProfileListRequest) {
    const response = await api.get<ApiListResponse<TeacherProfileRDO>>(
      apiRoutes.admin.teacherProfiles,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getTeacherProfile(profileId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminTeacherProfileDetailRDO>>(
      apiRoutes.admin.teacherProfile(profileId),
    )
    return unwrapResource(response)
  },

  async approveTeacherProfile(profileId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.admin.teacherProfileApprove(profileId),
    )
    return unwrapResource(response)
  },

  async rejectTeacherProfile(profileId: BackendId, payload: TeacherProfileModerationUDO) {
    const response = await api.post<ApiSuccessEnvelope<TeacherProfileRDO>>(
      apiRoutes.admin.teacherProfileReject(profileId),
      payload,
    )
    return unwrapResource(response)
  },

  async listModerationTests(filters?: AdminModerationTestListRequest) {
    const response = await api.get<ApiListResponse<AdminModerationTestRDO>>(
      apiRoutes.admin.moderationTests,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getModerationMeta() {
    const response = await api.get<ApiSuccessEnvelope<AdminModerationQueueMetaRDO>>(
      apiRoutes.admin.moderationTestsMeta,
    )
    return unwrapResource(response)
  },

  async getModerationTest(testId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminModerationTestDetailRDO>>(
      apiRoutes.admin.moderationTest(testId),
    )
    return unwrapResource(response)
  },

  async approveModerationTest(testId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<AdminModerationTestDetailRDO>>(
      apiRoutes.admin.moderationTestApprove(testId),
    )
    return unwrapResource(response)
  },

  async rejectModerationTest(testId: BackendId, payload: TestModerationRejectCDO) {
    const response = await api.post<ApiSuccessEnvelope<AdminModerationTestDetailRDO>>(
      apiRoutes.admin.moderationTestReject(testId),
      payload,
    )
    return unwrapResource(response)
  },

  async listPayments(filters?: AdminPaymentListRequest) {
    const response = await api.get<ApiListResponse<AdminPaymentRDO>>(
      apiRoutes.admin.payments,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getPayment(paymentId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminPaymentRDO>>(
      apiRoutes.admin.payment(paymentId),
    )
    return unwrapResource(response)
  },

  async listWithdrawals(filters?: AdminWithdrawalListRequest) {
    const response = await api.get<ApiListResponse<AdminWithdrawalRequestRDO>>(
      apiRoutes.admin.withdrawals,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getWithdrawal(withdrawalRequestId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminWithdrawalRequestRDO>>(
      apiRoutes.admin.withdrawal(withdrawalRequestId),
    )
    return unwrapResource(response)
  },

  async approveWithdrawal(withdrawalRequestId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<AdminWithdrawalRequestRDO>>(
      apiRoutes.admin.withdrawalApprove(withdrawalRequestId),
    )
    return unwrapResource(response)
  },

  async rejectWithdrawal(withdrawalRequestId: BackendId, payload: WithdrawalRejectionCDO) {
    const response = await api.post<ApiSuccessEnvelope<AdminWithdrawalRequestRDO>>(
      apiRoutes.admin.withdrawalReject(withdrawalRequestId),
      payload,
    )
    return unwrapResource(response)
  },

  async markWithdrawalPaid(withdrawalRequestId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<AdminWithdrawalRequestRDO>>(
      apiRoutes.admin.withdrawalMarkPaid(withdrawalRequestId),
    )
    return unwrapResource(response)
  },

  async listPaymentDisputes(filters?: AdminPaymentDisputeListRequest) {
    const response = await api.get<ApiListResponse<AdminPaymentDisputeRDO>>(
      apiRoutes.admin.paymentDisputes,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getPaymentDispute(disputeId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<AdminPaymentDisputeRDO>>(
      apiRoutes.admin.paymentDispute(disputeId),
    )
    return unwrapResource(response)
  },

  async decidePaymentDispute(disputeId: BackendId, payload: PaymentDisputeDecisionCDO) {
    const response = await api.post<ApiSuccessEnvelope<AdminPaymentDisputeRDO>>(
      apiRoutes.admin.paymentDisputeDecision(disputeId),
      payload,
    )
    return unwrapResource(response)
  },

  async listSettings() {
    const response = await api.get<ApiUnpagedListResponse<PlatformSettingRDO>>(
      apiRoutes.admin.settings,
    )
    return unwrapUnpagedListResponse(response)
  },

  async getSetting(key: string) {
    const response = await api.get<ApiSuccessEnvelope<PlatformSettingRDO>>(
      apiRoutes.admin.setting(key),
    )
    return unwrapResource(response)
  },

  async updateSetting(key: string, payload: PlatformSettingUDO) {
    const response = await api.patch<ApiSuccessEnvelope<PlatformSettingRDO>>(
      apiRoutes.admin.setting(key),
      payload,
    )
    return unwrapResource(response)
  },

  async getReport(name: AdminReportName, filters?: AdminReportRequest) {
    const response = await api.get<ApiSuccessEnvelope<AdminReportRDO>>(
      adminReportRoute(name),
      params(filters),
    )
    return unwrapResource(response)
  },
}

export const settingsService = {
  list: adminService.listSettings,
  get: adminService.getSetting,
  update: adminService.updateSetting,
}

export const scheduleService = {
  async listPublicSlots(filters?: LiveSpeakingSlotListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingSlotRDO>>(
      apiRoutes.student.schedule.slots,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async listStudentBookings(filters?: LiveSpeakingBookingListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingBookingRDO>>(
      apiRoutes.student.schedule.bookings,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getStudentBooking(bookingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.student.schedule.booking(bookingId),
    )
    return unwrapResource(response)
  },

  async reserveSlot(slotId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.student.schedule.reserveSlot(slotId),
    )
    return unwrapResource(response)
  },

  async createBookingPayment(bookingId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<PaymentRDO>>(
      apiRoutes.student.schedule.bookingPayment(bookingId),
    )
    return unwrapResource(response)
  },

  async cancelStudentBooking(bookingId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.student.schedule.cancelBooking(bookingId),
    )
    return unwrapResource(response)
  },

  async listTeacherSlots(filters?: LiveSpeakingSlotListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingSlotRDO>>(
      apiRoutes.teacher.schedule.slots,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async createTeacherSlot(payload: LiveSpeakingSlotCDO) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingSlotRDO>>(
      apiRoutes.teacher.schedule.slots,
      payload,
    )
    return unwrapResource(response)
  },

  async updateTeacherSlot(slotId: BackendId, payload: LiveSpeakingSlotUDO) {
    const response = await api.patch<ApiSuccessEnvelope<LiveSpeakingSlotRDO>>(
      apiRoutes.teacher.schedule.slot(slotId),
      payload,
    )
    return unwrapResource(response)
  },

  async announceTeacherSlot(slotId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingSlotRDO>>(
      apiRoutes.teacher.schedule.announceSlot(slotId),
    )
    return unwrapResource(response)
  },

  async unannounceTeacherSlot(slotId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingSlotRDO>>(
      apiRoutes.teacher.schedule.unannounceSlot(slotId),
    )
    return unwrapResource(response)
  },

  async listTeacherBookings(filters?: LiveSpeakingBookingListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingBookingRDO>>(
      apiRoutes.teacher.schedule.bookings,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getTeacherBooking(bookingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.teacher.schedule.booking(bookingId),
    )
    return unwrapResource(response)
  },

  async completeTeacherBooking(bookingId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.teacher.schedule.completeBooking(bookingId),
    )
    return unwrapResource(response)
  },

  async markTeacherBookingNoShow(bookingId: BackendId) {
    const response = await api.post<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.teacher.schedule.noShowBooking(bookingId),
    )
    return unwrapResource(response)
  },

  async listAdminBookings(filters?: LiveSpeakingBookingListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingBookingRDO>>(
      apiRoutes.admin.schedule.bookings,
      params(filters),
    )
    return unwrapPageResponse(response)
  },

  async getAdminBooking(bookingId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<LiveSpeakingBookingRDO>>(
      apiRoutes.admin.schedule.booking(bookingId),
    )
    return unwrapResource(response)
  },

  async listAdminProviderFailures(filters?: LiveSpeakingProviderFailureListRequest) {
    const response = await api.get<ApiListResponse<LiveSpeakingProviderFailureRDO>>(
      apiRoutes.admin.schedule.providerFailures,
      params(filters),
    )
    return unwrapPageResponse(response)
  },
}

export const fileService = {
  async uploadStudentFile(payload: FormData, options?: FileUploadOptions) {
    const response = await api.post<ApiSuccessEnvelope<FileRDO>>(
      apiRoutes.student.files,
      payload,
      { onUploadProgress: options?.onUploadProgress },
    )
    return unwrapResource(response)
  },

  async uploadTeacherFile(payload: FormData, options?: FileUploadOptions) {
    const response = await api.post<ApiSuccessEnvelope<FileRDO>>(
      apiRoutes.teacher.files,
      payload,
      { onUploadProgress: options?.onUploadProgress },
    )
    return unwrapResource(response)
  },

  async uploadAdminFile(payload: FormData, options?: FileUploadOptions) {
    const response = await api.post<ApiSuccessEnvelope<FileRDO>>(
      apiRoutes.admin.files,
      payload,
      { onUploadProgress: options?.onUploadProgress },
    )
    return unwrapResource(response)
  },

  async getDownloadUrl(fileId: BackendId) {
    const response = await api.get<ApiSuccessEnvelope<FileDownloadUrlRDO>>(
      apiRoutes.student.fileDownloadUrl(fileId),
    )
    return unwrapResource(response)
  },
}

export const backendService = {
  auth: authService,
  catalog: catalogService,
  attempts: attemptService,
  results: resultService,
  aiPractice: aiPracticeService,
  marketplace: marketplaceService,
  teacher: teacherService,
  admin: adminService,
  settings: settingsService,
  schedule: scheduleService,
  files: fileService,

  async listExams(): Promise<MockExamRDO[]> {
    const response = await api.get<MockExamRDO[]>(apiRoutes.public.compatibility.exams)
    return response.data
  },

  async getCurrentUserSession(area: 'student' | 'teacher'): Promise<ApiResourceResult<CurrentUserRDO>> {
    const route = area === 'student' ? apiRoutes.student.session : apiRoutes.teacher.session
    const response = await api.get<ApiSuccessEnvelope<CurrentUserRDO>>(route)
    return unwrapResourceResponse(response)
  },
}
