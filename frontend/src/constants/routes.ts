export const ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  tests: '/tests',
  testDetail: '/tests/:id',
  marketplace: '/marketplace',
  marketplaceListing: '/marketplace/:id',
  teacherPublicProfile: '/teachers/:id',
  pricing: '/pricing',
  dashboard: '/dashboard',
  resultDetail: '/results/:id',
  writing: '/writing',
  speaking: '/speaking',
  profile: '/profile',
  teacherDashboard: '/teacher/dashboard',
  teacherTests: '/teacher/tests',
  teacherTestCreate: '/teacher/tests/create',
  teacherTestEdit: '/teacher/tests/:id/edit',
  teacherSchedule: '/teacher/schedule',
  teacherEarnings: '/teacher/earnings',
  teacherStudents: '/teacher/students',
  teacherNotifications: '/teacher/notifications',
  teacherProfile: '/teacher/profile',
  adminDashboard: '/admin/dashboard',
  adminUsers: '/admin/users',
  adminTeachers: '/admin/teachers',
  adminTests: '/admin/tests',
  adminModeration: '/admin/moderation',
  adminPayments: '/admin/payments',
  adminWithdrawals: '/admin/withdrawals',
  adminReports: '/admin/reports',
  adminSettings: '/admin/settings',
} as const

export type AppRouteKey = keyof typeof ROUTES

export function fillRoute(
  route: string,
  params: Record<string, string | number>,
) {
  return Object.entries(params).reduce(
    (path, [key, value]) => path.replace(`:${key}`, String(value)),
    route,
  )
}
