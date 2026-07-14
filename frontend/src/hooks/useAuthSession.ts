import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { getDefaultAuthenticatedRoute } from '../constants/navigation'
import { ROUTES } from '../constants/routes'
import { authKeys } from './queryKeys'
import { authService } from '../services/auth.service'
import { useSessionStore } from '../store/sessionStore'
import {
  getApiErrorMessage,
  isAccountInactiveError,
  isSessionExpiryError,
} from '../utils/apiErrors'

export { authKeys } from './queryKeys'

export function useCurrentUserBootstrap() {
  const accessToken = useSessionStore((session) => session.accessToken)
  const setCurrentUser = useSessionStore((session) => session.setCurrentUser)
  const clearSession = useSessionStore((session) => session.clearSession)
  const shouldBootstrap = Boolean(accessToken)

  const query = useQuery({
    queryKey: authKeys.currentUser,
    queryFn: authService.getCurrentUser,
    enabled: shouldBootstrap,
    retry: false,
    staleTime: 60_000,
  })

  useEffect(() => {
    if (query.data) {
      setCurrentUser(query.data)
    }
  }, [query.data, setCurrentUser])

  useEffect(() => {
    if (
      shouldBootstrap &&
      query.isError &&
      (isSessionExpiryError(query.error) || isAccountInactiveError(query.error))
    ) {
      clearSession()
    }
  }, [clearSession, query.error, query.isError, shouldBootstrap])

  return query
}

export function useLogout() {
  const clearSession = useSessionStore((session) => session.clearSession)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return () => {
    clearSession()
    queryClient.clear()
    navigate(ROUTES.login, { replace: true })
  }
}

export function getAuthErrorMessage(error: unknown, fallbackMessage: string) {
  return getApiErrorMessage(error, fallbackMessage)
}

export function getAuthenticatedRedirectPath() {
  return getDefaultAuthenticatedRoute(useSessionStore.getState())
}
