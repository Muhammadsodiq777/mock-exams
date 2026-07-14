import { ROUTES } from './routes'
import type { AppRole, AppSession } from '../types/session'
import { hasAnyRole, hasApprovedTeacherProfile, isTeacherAwaitingApproval } from '../types/session'

export type NavigationItem = {
  label: string
  to: string
  roles?: AppRole[]
  auth?: 'public' | 'authenticated'
  requiresApprovedTeacher?: boolean
}

export const publicNavigationItems = [
  { label: 'Tests', to: ROUTES.tests, auth: 'public' },
  { label: 'Marketplace', to: ROUTES.marketplace, auth: 'public' },
  { label: 'Pricing', to: ROUTES.pricing, auth: 'public' },
] satisfies NavigationItem[]

export const appNavigationItems = [
  { label: 'Dashboard', to: ROUTES.dashboard, roles: ['student'] },
  { label: 'Writing', to: ROUTES.writing, roles: ['student'] },
  { label: 'Speaking', to: ROUTES.speaking, roles: ['student'] },
  { label: 'Profile', to: ROUTES.profile, roles: ['student'] },
  { label: 'Teacher', to: ROUTES.teacherDashboard, roles: ['teacher'] },
  { label: 'Admin', to: ROUTES.adminDashboard, roles: ['admin'] },
] satisfies NavigationItem[]

export const teacherNavigationItems = [
  { label: 'Dashboard', to: ROUTES.teacherDashboard },
  { label: 'Tests', to: ROUTES.teacherTests, requiresApprovedTeacher: true },
  { label: 'Schedule', to: ROUTES.teacherSchedule, requiresApprovedTeacher: true },
  { label: 'Earnings', to: ROUTES.teacherEarnings, requiresApprovedTeacher: true },
  { label: 'Students', to: ROUTES.teacherStudents, requiresApprovedTeacher: true },
  { label: 'Notifications', to: ROUTES.teacherNotifications, requiresApprovedTeacher: true },
  { label: 'Profile', to: ROUTES.teacherProfile },
] satisfies NavigationItem[]

export const adminNavigationItems = [
  { label: 'Dashboard', to: ROUTES.adminDashboard },
  { label: 'Users', to: ROUTES.adminUsers },
  { label: 'Teachers', to: ROUTES.adminTeachers },
  { label: 'Moderation', to: ROUTES.adminModeration },
  { label: 'Payments', to: ROUTES.adminPayments },
  { label: 'Withdrawals', to: ROUTES.adminWithdrawals },
  { label: 'Reports', to: ROUTES.adminReports },
  { label: 'Settings', to: ROUTES.adminSettings },
] satisfies NavigationItem[]

export function getVisibleNavigationItems(
  items: NavigationItem[],
  session: AppSession,
) {
  return items.filter((item) => {
    if (item.auth === 'authenticated' && session.status !== 'authenticated') {
      return false
    }

    if (item.roles) {
      if (session.status !== 'authenticated') {
        return false
      }

      if (!hasAnyRole(session.user, item.roles)) {
        return false
      }
    }

    if (item.requiresApprovedTeacher) {
      if (session.status !== 'authenticated') {
        return false
      }

      return hasApprovedTeacherProfile(session.user)
    }

    return true
  })
}

export function getDefaultAuthenticatedRoute(session: AppSession) {
  if (session.status !== 'authenticated') {
    return ROUTES.login
  }

  if (session.user.roles.includes('admin')) {
    return ROUTES.adminDashboard
  }

  if (session.user.roles.includes('teacher')) {
    return isTeacherAwaitingApproval(session.user) ? ROUTES.teacherProfile : ROUTES.teacherDashboard
  }

  return ROUTES.dashboard
}
