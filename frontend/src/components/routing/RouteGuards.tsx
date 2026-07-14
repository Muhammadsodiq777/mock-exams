import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { ROUTES } from '../../constants/routes'
import { useSessionStore } from '../../store/sessionStore'
import type { AppRole } from '../../types/session'
import { hasAnyRole, hasApprovedTeacherProfile } from '../../types/session'
import { ForbiddenState, SessionLoadingState } from './ShellState'

type RequireRoleProps = {
  allowedRoles: AppRole[]
}

export function RequireAuth() {
  const location = useLocation()
  const session = useSessionStore()

  if (session.status === 'loading') {
    return <SessionLoadingState />
  }

  if (session.status === 'anonymous') {
    return <Navigate replace to={ROUTES.login} state={{ from: location }} />
  }

  return <Outlet />
}

export function RequireRole({ allowedRoles }: RequireRoleProps) {
  const session = useSessionStore()

  if (session.status === 'loading') {
    return <SessionLoadingState />
  }

  if (session.status !== 'authenticated') {
    return <Navigate replace to={ROUTES.login} />
  }

  if (!hasAnyRole(session.user, allowedRoles)) {
    return <ForbiddenState />
  }

  return <Outlet />
}

export function RequireApprovedTeacher() {
  const session = useSessionStore()

  if (session.status === 'loading') {
    return <SessionLoadingState />
  }

  if (session.status !== 'authenticated') {
    return <Navigate replace to={ROUTES.login} />
  }

  if (!hasApprovedTeacherProfile(session.user)) {
    return <Navigate replace to={ROUTES.teacherProfile} state={{ needsTeacherApproval: true }} />
  }

  return <Outlet />
}
