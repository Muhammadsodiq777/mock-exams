import type { ReactNode } from 'react'

import { useCurrentUserBootstrap } from '../../hooks/useAuthSession'

type SessionBootstrapProps = {
  children: ReactNode
}

export function SessionBootstrap({ children }: SessionBootstrapProps) {
  useCurrentUserBootstrap()

  return children
}
