import { create } from 'zustand'

import type { AuthSessionRDO } from '../types/backend'
import type { AppSession, AppUser } from '../types/session'
import { mapAuthSessionToAppSession } from '../types/session'

type SessionStore = AppSession & {
  setLoading: () => void
  setAnonymous: () => void
  setAuthenticated: (session: AuthSessionRDO) => void
  setCurrentUser: (user: AppUser) => void
  clearSession: () => void
}

const tokenStorageKey = 'mock-exams.auth'

type PersistedSession = {
  accessToken: string
  expiresAt: string
}

const anonymousSession = {
  status: 'anonymous',
  user: null,
  accessToken: null,
  expiresAt: null,
} as const

const readPersistedSession = (): PersistedSession | null => {
  if (typeof window === 'undefined') {
    return null
  }

  const rawValue = window.localStorage.getItem(tokenStorageKey)

  if (!rawValue) {
    return null
  }

  try {
    const parsedValue = JSON.parse(rawValue) as Partial<PersistedSession>

    if (typeof parsedValue.accessToken !== 'string' || !parsedValue.accessToken) {
      return null
    }

    if (typeof parsedValue.expiresAt !== 'string' || !parsedValue.expiresAt) {
      return null
    }

    return {
      accessToken: parsedValue.accessToken,
      expiresAt: parsedValue.expiresAt,
    }
  } catch {
    window.localStorage.removeItem(tokenStorageKey)
    return null
  }
}

const persistSession = (session: AuthSessionRDO) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(
    tokenStorageKey,
    JSON.stringify({
      accessToken: session.accessToken,
      expiresAt: session.expiresAt,
    } satisfies PersistedSession),
  )
}

const clearPersistedSession = () => {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(tokenStorageKey)
  }
}

const persistedSession = readPersistedSession()

export const useSessionStore = create<SessionStore>((set) => ({
  ...(persistedSession
    ? {
        status: 'loading',
        user: null,
        accessToken: persistedSession.accessToken,
        expiresAt: persistedSession.expiresAt,
      }
    : anonymousSession),
  setLoading: () => {
    const accessToken = useSessionStore.getState().accessToken
    const expiresAt = useSessionStore.getState().expiresAt

    if (!accessToken || !expiresAt) {
      set(anonymousSession)
      return
    }

    set({ status: 'loading', user: null, accessToken, expiresAt })
  },
  setAnonymous: () => {
    clearPersistedSession()
    set(anonymousSession)
  },
  setAuthenticated: (session) => {
    persistSession(session)
    set(mapAuthSessionToAppSession(session))
  },
  setCurrentUser: (user) => {
    const { accessToken, expiresAt } = useSessionStore.getState()

    if (!accessToken || !expiresAt) {
      set(anonymousSession)
      return
    }

    set({
      status: 'authenticated',
      user,
      accessToken,
      expiresAt,
    })
  },
  clearSession: () => {
    clearPersistedSession()
    set(anonymousSession)
  },
}))
