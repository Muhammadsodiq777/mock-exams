import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { EmptyState } from '../ui'

type ShellStateProps = {
  eyebrow?: string
  title: string
  message: string
  actions?: ReactNode
}

export function ShellState({ actions, eyebrow, message, title }: ShellStateProps) {
  return (
    <EmptyState
      actions={actions}
      className="shell-state"
      eyebrow={eyebrow}
      message={message}
      title={title}
    />
  )
}

export function SessionLoadingState() {
  return (
    <ShellState
      eyebrow="Session"
      title="Checking your session"
      message="Hold on while the app confirms where to send you."
    />
  )
}

export function ForbiddenState() {
  return (
    <ShellState
      eyebrow="Access denied"
      title="This area is not available for your account"
      message="Your account is signed in, but this route belongs to another role or requires an approval you do not have yet."
      actions={<Link to="/">Back to public pages</Link>}
    />
  )
}

export function NotFoundState() {
  return (
    <ShellState
      eyebrow="Not found"
      title="We could not find that page"
      message="The route may be incomplete, moved, or not part of this app shell."
      actions={<Link to="/">Back to home</Link>}
    />
  )
}
