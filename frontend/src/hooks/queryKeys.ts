import type {
  AdminModerationTestListRequest,
  AdminPaymentDisputeListRequest,
  AdminPaymentListRequest,
  AdminReportName,
  AdminReportRequest,
  AdminTeacherProfileListRequest,
  AdminUserListRequest,
  AdminWithdrawalListRequest,
  BackendId,
  LiveSpeakingBookingListRequest,
  LiveSpeakingProviderFailureListRequest,
  LiveSpeakingSlotListRequest,
  MarketplaceListingRequest,
  PageRequest,
  TeacherWithdrawalListRequest,
  TestCatalogRequest,
  TeacherTestListRequest,
} from '../types/backend'

const compactParams = <TParams extends Record<string, unknown> | undefined>(params?: TParams) =>
  params ? ({ ...params } as const) : undefined

export const authKeys = {
  all: ['auth'] as const,
  currentUser: ['auth', 'me'] as const,
}

export const catalogKeys = {
  all: ['catalog'] as const,
  tests: (filters?: TestCatalogRequest) =>
    ['catalog', 'tests', compactParams(filters)] as const,
  test: (testId: BackendId) => ['catalog', 'tests', testId] as const,
  categories: ['catalog', 'categories'] as const,
  tags: ['catalog', 'tags'] as const,
  teacherProfiles: (filters?: PageRequest) =>
    ['catalog', 'teacher-profiles', compactParams(filters)] as const,
  teacherProfile: (profileId: BackendId) =>
    ['catalog', 'teacher-profiles', profileId] as const,
}

export const mockExamKeys = {
  all: ['mock-exams'] as const,
}

export const attemptKeys = {
  all: ['attempts'] as const,
  detail: (attemptId: BackendId) => ['attempts', attemptId] as const,
  result: (attemptId: BackendId) => ['attempts', attemptId, 'result'] as const,
}

export const resultKeys = {
  all: ['results'] as const,
  progress: (filters?: { days?: number }) =>
    ['results', 'progress', compactParams(filters)] as const,
  detail: (resultId: BackendId) => ['results', resultId] as const,
}

export const fileKeys = {
  all: ['files'] as const,
  downloadUrl: (fileId: BackendId) => ['files', fileId, 'download-url'] as const,
}

export const aiPracticeKeys = {
  all: ['ai-practice'] as const,
  speakingSubmission: (submissionId: BackendId) =>
    ['ai-practice', 'speaking-submissions', submissionId] as const,
  speakingFeedback: (submissionId: BackendId) =>
    ['ai-practice', 'speaking-submissions', submissionId, 'feedback'] as const,
  writingSubmission: (submissionId: BackendId) =>
    ['ai-practice', 'writing-submissions', submissionId] as const,
  writingFeedback: (submissionId: BackendId) =>
    ['ai-practice', 'writing-submissions', submissionId, 'feedback'] as const,
  aiUsage: ['ai-practice', 'usage'] as const,
  activeStudyPlan: ['ai-practice', 'study-plans', 'active'] as const,
  studyPlan: (planId: BackendId) => ['ai-practice', 'study-plans', planId] as const,
  studyPlanHistory: (filters?: PageRequest) =>
    ['ai-practice', 'study-plans', 'history', compactParams(filters)] as const,
}

export const marketplaceKeys = {
  all: ['marketplace'] as const,
  publicListings: (filters?: MarketplaceListingRequest) =>
    ['marketplace', 'public-listings', compactParams(filters)] as const,
  publicListing: (listingId: BackendId) =>
    ['marketplace', 'public-listings', listingId] as const,
  listingAccess: (listingId: BackendId) =>
    ['marketplace', 'public-listings', listingId, 'access'] as const,
  listingItems: (listingId: BackendId) =>
    ['marketplace', 'public-listings', listingId, 'items'] as const,
  purchases: (filters?: PageRequest) =>
    ['marketplace', 'purchases', compactParams(filters)] as const,
  purchase: (purchaseId: BackendId) => ['marketplace', 'purchases', purchaseId] as const,
  payments: ['marketplace', 'payments'] as const,
  payment: (paymentId: BackendId) => ['marketplace', 'payments', paymentId] as const,
  teacherListings: (filters?: PageRequest) =>
    ['marketplace', 'teacher-listings', compactParams(filters)] as const,
}

export const pricingKeys = {
  all: ['pricing'] as const,
  listings: (filters?: MarketplaceListingRequest) =>
    ['pricing', 'listings', compactParams(filters)] as const,
}

export const teacherKeys = {
  all: ['teacher'] as const,
  dashboard: ['teacher', 'dashboard'] as const,
  profile: ['teacher', 'profile'] as const,
  testsRoot: ['teacher', 'tests'] as const,
  tests: (filters?: TeacherTestListRequest) => ['teacher', 'tests', compactParams(filters)] as const,
  test: (testId: BackendId) => ['teacher', 'tests', testId] as const,
  testPreview: (testId: BackendId) => ['teacher', 'tests', testId, 'preview'] as const,
  testSections: (testId: BackendId) => ['teacher', 'tests', testId, 'sections'] as const,
  wallet: ['teacher', 'wallet'] as const,
  walletTransactionsRoot: ['teacher', 'wallet', 'transactions'] as const,
  walletTransactions: (filters?: PageRequest) =>
    ['teacher', 'wallet', 'transactions', compactParams(filters)] as const,
  withdrawalsRoot: ['teacher', 'wallet', 'withdrawals'] as const,
  withdrawals: (filters?: TeacherWithdrawalListRequest) =>
    ['teacher', 'wallet', 'withdrawals', compactParams(filters)] as const,
  withdrawal: (withdrawalRequestId: BackendId) =>
    ['teacher', 'wallet', 'withdrawals', withdrawalRequestId] as const,
  reviews: (filters?: PageRequest) => ['teacher', 'reviews', compactParams(filters)] as const,
  reviewSummary: ['teacher', 'reviews', 'summary'] as const,
  notificationsRoot: ['teacher', 'notifications'] as const,
  notifications: (filters?: PageRequest) =>
    ['teacher', 'notifications', compactParams(filters)] as const,
  notification: (notificationId: BackendId) =>
    ['teacher', 'notifications', notificationId] as const,
  notificationUnreadCount: ['teacher', 'notifications', 'unread-count'] as const,
}

export const adminKeys = {
  all: ['admin'] as const,
  dashboard: (filters?: Record<string, unknown>) =>
    ['admin', 'dashboard', compactParams(filters)] as const,
  settings: ['admin', 'settings'] as const,
  setting: (key: string) => ['admin', 'settings', key] as const,
  users: (filters?: AdminUserListRequest) => ['admin', 'users', compactParams(filters)] as const,
  user: (userId: BackendId) => ['admin', 'users', userId] as const,
  teacherProfiles: (filters?: AdminTeacherProfileListRequest) =>
    ['admin', 'teacher-profiles', compactParams(filters)] as const,
  teacherProfile: (profileId: BackendId) => ['admin', 'teacher-profiles', profileId] as const,
  moderationTests: (filters?: AdminModerationTestListRequest) =>
    ['admin', 'moderation', 'tests', compactParams(filters)] as const,
  moderationTestsMeta: ['admin', 'moderation', 'tests', 'meta'] as const,
  moderationTest: (testId: BackendId) => ['admin', 'moderation', 'tests', testId] as const,
  payments: (filters?: AdminPaymentListRequest) =>
    ['admin', 'payments', compactParams(filters)] as const,
  payment: (paymentId: BackendId) => ['admin', 'payments', paymentId] as const,
  withdrawals: (filters?: AdminWithdrawalListRequest) =>
    ['admin', 'withdrawals', compactParams(filters)] as const,
  withdrawal: (withdrawalRequestId: BackendId) =>
    ['admin', 'withdrawals', withdrawalRequestId] as const,
  paymentDisputes: (filters?: AdminPaymentDisputeListRequest) =>
    ['admin', 'payment-disputes', compactParams(filters)] as const,
  paymentDispute: (disputeId: BackendId) =>
    ['admin', 'payment-disputes', disputeId] as const,
  reports: (name: AdminReportName, filters?: AdminReportRequest) =>
    ['admin', 'reports', name, compactParams(filters)] as const,
}

export const settingsKeys = {
  all: adminKeys.settings,
  detail: adminKeys.setting,
}

export const scheduleKeys = {
  all: ['schedule'] as const,
  publicSlots: (filters?: LiveSpeakingSlotListRequest) =>
    ['schedule', 'public-slots', compactParams(filters)] as const,
  studentBookings: (filters?: LiveSpeakingBookingListRequest) =>
    ['schedule', 'student-bookings', compactParams(filters)] as const,
  studentBooking: (bookingId: BackendId) =>
    ['schedule', 'student-bookings', bookingId] as const,
  teacherSlots: (filters?: LiveSpeakingSlotListRequest) =>
    ['schedule', 'teacher-slots', compactParams(filters)] as const,
  teacherBookings: (filters?: LiveSpeakingBookingListRequest) =>
    ['schedule', 'teacher-bookings', compactParams(filters)] as const,
  teacherBooking: (bookingId: BackendId) =>
    ['schedule', 'teacher-bookings', bookingId] as const,
  adminBookings: (filters?: LiveSpeakingBookingListRequest) =>
    ['schedule', 'admin-bookings', compactParams(filters)] as const,
  adminBooking: (bookingId: BackendId) =>
    ['schedule', 'admin-bookings', bookingId] as const,
  adminProviderFailures: (filters?: LiveSpeakingProviderFailureListRequest) =>
    ['schedule', 'admin-provider-failures', compactParams(filters)] as const,
}
