import type { AuthSessionRDO, CurrentUserRDO, RoleIdentifier } from './backend'

export type AppRole = RoleIdentifier

export type AppUser = CurrentUserRDO

export type SessionStatus = 'loading' | 'anonymous' | 'authenticated'

export type AppSession =
  | {
      status: 'loading'
      user: null
      accessToken: string
      expiresAt: string | null
    }
  | {
      status: 'anonymous'
      user: null
      accessToken: null
      expiresAt: null
    }
  | {
      status: 'authenticated'
      user: AppUser
      accessToken: string
      expiresAt: string
    }

export function hasAnyRole(user: AppUser, roles: AppRole[]) {
  return roles.some((role) => user.roles.includes(role))
}

export function hasApprovedTeacherProfile(user: AppUser) {
  return user.teacherProfile?.status === 'approved'
}

export function isTeacherAwaitingApproval(user: AppUser) {
  return user.roles.includes('teacher') && !hasApprovedTeacherProfile(user)
}

export function mapAuthSessionToAppSession(session: AuthSessionRDO): Extract<AppSession, { status: 'authenticated' }> {
  return {
    status: 'authenticated',
    accessToken: session.accessToken,
    expiresAt: session.expiresAt,
    user: session.user,
  }
}
