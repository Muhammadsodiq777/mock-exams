import type { BackendId } from '../types/backend'

type PathParam = BackendId | string

const byId = (basePath: string, id: PathParam) => `${basePath}/${id}`

export const apiRoutes = {
  public: {
    tests: '/public/tests',
    test: (testId: PathParam) => byId('/public/tests', testId),
    testCategories: '/public/test-categories',
    testTags: '/public/test-tags',
    teacherProfiles: '/public/teacher-profiles',
    teacherProfile: (profileId: PathParam) => byId('/public/teacher-profiles', profileId),
    marketplace: '/public/marketplace',
    marketplaceListing: (listingId: PathParam) => byId('/public/marketplace', listingId),
    testReviews: (testId: PathParam) => `${byId('/public/tests', testId)}/reviews`,
    teacherProfileReviews: (profileId: PathParam) =>
      `${byId('/public/teacher-profiles', profileId)}/reviews`,
    compatibility: {
      exams: '/exams',
    },
  },
  student: {
    session: '/student/session',
    progress: '/student/progress',
    purchases: '/student/purchases',
    purchase: (purchaseId: PathParam) => byId('/student/purchases', purchaseId),
    payments: '/student/payments',
    payment: (paymentId: PathParam) => byId('/student/payments', paymentId),
    promoQuote: '/student/payments/promo-quote',
    paymentDisputes: '/student/payment-disputes',
    paymentDispute: (disputeId: PathParam) => byId('/student/payment-disputes', disputeId),
    marketplaceFreeAccess: (listingId: PathParam) =>
      `${byId('/student/marketplace/listings', listingId)}/free-access`,
    marketplaceAccess: (listingId: PathParam) =>
      `${byId('/student/marketplace/listings', listingId)}/access`,
    marketplaceItems: (listingId: PathParam) =>
      `${byId('/student/marketplace/listings', listingId)}/items`,
    testReviews: (testId: PathParam) => `${byId('/student/tests', testId)}/reviews`,
    review: (reviewId: PathParam) => byId('/student/reviews', reviewId),
    testAttempts: (testId: PathParam) => `${byId('/student/tests', testId)}/attempts`,
    attemptAnswers: (attemptId: PathParam) =>
      `${byId('/student/attempts', attemptId)}/answers`,
    attemptSubmit: (attemptId: PathParam) => `${byId('/student/attempts', attemptId)}/submit`,
    attemptResult: (attemptId: PathParam) => `${byId('/student/attempts', attemptId)}/result`,
    result: (resultId: PathParam) => byId('/student/results', resultId),
    speakingSubmissions: '/student/speaking-submissions',
    speakingSubmission: (submissionId: PathParam) =>
      byId('/student/speaking-submissions', submissionId),
    speakingTranscription: (submissionId: PathParam) =>
      `${byId('/student/speaking-submissions', submissionId)}/transcription`,
    speakingFeedback: (submissionId: PathParam) =>
      `${byId('/student/speaking-submissions', submissionId)}/feedback`,
    speakingFeedbackRetry: (submissionId: PathParam) =>
      `${byId('/student/speaking-submissions', submissionId)}/feedback/retry`,
    writingSubmissions: '/student/writing-submissions',
    writingSubmission: (submissionId: PathParam) =>
      byId('/student/writing-submissions', submissionId),
    writingFeedback: (submissionId: PathParam) =>
      `${byId('/student/writing-submissions', submissionId)}/feedback`,
    writingFeedbackRetry: (submissionId: PathParam) =>
      `${byId('/student/writing-submissions', submissionId)}/feedback/retry`,
    aiUsage: '/student/ai-usage',
    studyPlans: '/student/study-plans',
    studyPlan: (planId: PathParam) => byId('/student/study-plans', planId),
    activeStudyPlan: '/student/study-plans/active',
    studyPlanHistory: '/student/study-plans/history',
    regenerateStudyPlan: (planId: PathParam) =>
      `${byId('/student/study-plans', planId)}/regenerate`,
    completeStudyPlanTask: (planId: PathParam, taskId: PathParam) =>
      `${byId('/student/study-plans', planId)}/tasks/${taskId}/complete`,
    uncompleteStudyPlanTask: (planId: PathParam, taskId: PathParam) =>
      `${byId('/student/study-plans', planId)}/tasks/${taskId}/uncomplete`,
    files: '/student/files',
    fileDownloadUrl: (fileId: PathParam) => byId('/files', `${fileId}/download-url`),
    notifications: '/student/notifications',
    notificationRead: (notificationId: PathParam) =>
      `${byId('/student/notifications', notificationId)}/read`,
    notificationUnreadCount: '/student/notifications/unread-count',
    schedule: {
      slots: '/student/schedule/slots',
      bookings: '/student/schedule/bookings',
      booking: (bookingId: PathParam) => byId('/student/schedule/bookings', bookingId),
      reserveSlot: (slotId: PathParam) => `${byId('/student/schedule/slots', slotId)}/reserve`,
      bookingPayment: (bookingId: PathParam) =>
        `${byId('/student/schedule/bookings', bookingId)}/payment`,
      cancelBooking: (bookingId: PathParam) =>
        `${byId('/student/schedule/bookings', bookingId)}/cancel`,
    },
  },
  teacher: {
    dashboard: '/teacher/dashboard',
    session: '/teacher/session',
    profile: '/teacher/profile',
    profileSubmit: '/teacher/profile/submit',
    tests: '/teacher/tests',
    test: (testId: PathParam) => byId('/teacher/tests', testId),
    testPreview: (testId: PathParam) => `${byId('/teacher/tests', testId)}/preview`,
    testSubmit: (testId: PathParam) => `${byId('/teacher/tests', testId)}/submit`,
    testArchive: (testId: PathParam) => `${byId('/teacher/tests', testId)}/archive`,
    testSections: (testId: PathParam) => `${byId('/teacher/tests', testId)}/sections`,
    testSection: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}`,
    testSectionsReorder: (testId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/reorder`,
    testSectionParagraphs: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/paragraphs`,
    testSectionParagraph: (testId: PathParam, sectionId: PathParam, paragraphId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/paragraphs/${paragraphId}`,
    testSectionParagraphsReorder: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/paragraphs/reorder`,
    testSectionListeningSegments: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/listening-segments`,
    testSectionListeningSegment: (
      testId: PathParam,
      sectionId: PathParam,
      segmentId: PathParam,
    ) => `${byId('/teacher/tests', testId)}/sections/${sectionId}/listening-segments/${segmentId}`,
    testSectionListeningSegmentsReorder: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/listening-segments/reorder`,
    testSectionQuestions: (testId: PathParam, sectionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/questions`,
    testSectionQuestion: (testId: PathParam, sectionId: PathParam, questionId: PathParam) =>
      `${byId('/teacher/tests', testId)}/sections/${sectionId}/questions/${questionId}`,
    marketplaceListings: '/teacher/marketplace/listings',
    marketplaceListing: (listingId: PathParam) =>
      byId('/teacher/marketplace/listings', listingId),
    marketplaceListingItems: (listingId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/items`,
    marketplaceListingItem: (listingId: PathParam, itemId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/items/${itemId}`,
    marketplaceListingActivation: (listingId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/activation`,
    marketplaceListingActivate: (listingId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/activate`,
    marketplaceListingDeactivate: (listingId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/deactivate`,
    marketplaceListingPreview: (listingId: PathParam) =>
      `${byId('/teacher/marketplace/listings', listingId)}/preview`,
    wallet: '/teacher/wallet',
    walletTransactions: '/teacher/wallet/transactions',
    withdrawals: '/teacher/wallet/withdrawals',
    withdrawal: (withdrawalRequestId: PathParam) =>
      byId('/teacher/wallet/withdrawals', withdrawalRequestId),
    withdrawalCancel: (withdrawalRequestId: PathParam) =>
      `${byId('/teacher/wallet/withdrawals', withdrawalRequestId)}/cancel`,
    promoCodes: '/teacher/promo-codes',
    promoCode: (promoCodeId: PathParam) => byId('/teacher/promo-codes', promoCodeId),
    reviews: '/teacher/reviews',
    reviewSummary: '/teacher/reviews/summary',
    files: '/teacher/files',
    notifications: '/teacher/notifications',
    notificationRead: (notificationId: PathParam) =>
      `${byId('/teacher/notifications', notificationId)}/read`,
    notificationUnreadCount: '/teacher/notifications/unread-count',
    schedule: {
      slots: '/teacher/schedule/slots',
      slot: (slotId: PathParam) => byId('/teacher/schedule/slots', slotId),
      announceSlot: (slotId: PathParam) => `${byId('/teacher/schedule/slots', slotId)}/announce`,
      unannounceSlot: (slotId: PathParam) =>
        `${byId('/teacher/schedule/slots', slotId)}/unannounce`,
      bookings: '/teacher/schedule/bookings',
      booking: (bookingId: PathParam) => byId('/teacher/schedule/bookings', bookingId),
      completeBooking: (bookingId: PathParam) =>
        `${byId('/teacher/schedule/bookings', bookingId)}/complete`,
      noShowBooking: (bookingId: PathParam) =>
        `${byId('/teacher/schedule/bookings', bookingId)}/no-show`,
    },
  },
  admin: {
    dashboard: '/admin/dashboard',
    settings: '/admin/settings',
    setting: (key: string) => byId('/admin/settings', encodeURIComponent(key)),
    users: '/admin/users',
    user: (userId: PathParam) => byId('/admin/users', userId),
    userStatus: (userId: PathParam) => `${byId('/admin/users', userId)}/status`,
    teacherProfiles: '/admin/teacher-profiles',
    teacherProfile: (profileId: PathParam) => byId('/admin/teacher-profiles', profileId),
    teacherProfileApprove: (profileId: PathParam) =>
      `${byId('/admin/teacher-profiles', profileId)}/approve`,
    teacherProfileReject: (profileId: PathParam) =>
      `${byId('/admin/teacher-profiles', profileId)}/reject`,
    teacherProfileSuspend: (profileId: PathParam) =>
      `${byId('/admin/teacher-profiles', profileId)}/suspend`,
    teacherProfileReactivate: (profileId: PathParam) =>
      `${byId('/admin/teacher-profiles', profileId)}/reactivate`,
    moderationTests: '/admin/moderation/tests',
    moderationTestsMeta: '/admin/moderation/tests/meta',
    moderationTest: (testId: PathParam) => byId('/admin/moderation/tests', testId),
    moderationTestApprove: (testId: PathParam) =>
      `${byId('/admin/moderation/tests', testId)}/approve`,
    moderationTestReject: (testId: PathParam) =>
      `${byId('/admin/moderation/tests', testId)}/reject`,
    payments: '/admin/payments',
    payment: (paymentId: PathParam) => byId('/admin/payments', paymentId),
    withdrawals: '/admin/withdrawals',
    withdrawal: (withdrawalRequestId: PathParam) =>
      byId('/admin/withdrawals', withdrawalRequestId),
    withdrawalApprove: (withdrawalRequestId: PathParam) =>
      `${byId('/admin/withdrawals', withdrawalRequestId)}/approve`,
    withdrawalReject: (withdrawalRequestId: PathParam) =>
      `${byId('/admin/withdrawals', withdrawalRequestId)}/reject`,
    withdrawalMarkPaid: (withdrawalRequestId: PathParam) =>
      `${byId('/admin/withdrawals', withdrawalRequestId)}/mark-paid`,
    paymentDisputes: '/admin/payment-disputes',
    paymentDispute: (disputeId: PathParam) => byId('/admin/payment-disputes', disputeId),
    paymentDisputeDecision: (disputeId: PathParam) =>
      `${byId('/admin/payment-disputes', disputeId)}/decision`,
    promoCodes: '/admin/promo-codes',
    promoCode: (promoCodeId: PathParam) => byId('/admin/promo-codes', promoCodeId),
    reviews: '/admin/reviews',
    reviewHide: (reviewId: PathParam) => `${byId('/admin/reviews', reviewId)}/hide`,
    reviewUnhide: (reviewId: PathParam) => `${byId('/admin/reviews', reviewId)}/unhide`,
    review: (reviewId: PathParam) => byId('/admin/reviews', reviewId),
    reports: {
      aiUsage: '/admin/reports/ai-usage',
      sales: '/admin/reports/sales',
      revenue: '/admin/reports/revenue',
      testActivity: '/admin/reports/test-activity',
      providerFailures: '/admin/reports/provider-failures',
    },
    aiUsageReport: '/admin/ai-usage/report',
    aiUsageReportTotals: '/admin/ai-usage/report/totals',
    files: '/admin/files',
    notifications: '/admin/notifications',
    notificationRead: (notificationId: PathParam) =>
      `${byId('/admin/notifications', notificationId)}/read`,
    notificationUnreadCount: '/admin/notifications/unread-count',
    schedule: {
      bookings: '/admin/schedule/bookings',
      booking: (bookingId: PathParam) => byId('/admin/schedule/bookings', bookingId),
      providerFailures: '/admin/schedule/provider-failures',
    },
  },
  auth: {
    registerStudent: '/auth/register/student',
    registerTeacher: '/auth/register/teacher',
    login: '/auth/login',
    me: '/auth/me',
    refresh: '/auth/refresh',
    logout: '/auth/logout',
  },
} as const
