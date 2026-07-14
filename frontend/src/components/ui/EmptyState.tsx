import { type ReactNode, useId } from 'react'

import { cx } from './shared'

export type EmptyStateProps = {
  actions?: ReactNode
  className?: string
  eyebrow?: string
  message: string
  title: string
}

export function EmptyState({ actions, className, eyebrow, message, title }: EmptyStateProps) {
  const titleId = useId()

  return (
    <section className={cx('ui-empty-state', className)} aria-labelledby={titleId}>
      {eyebrow && <p className="ui-eyebrow">{eyebrow}</p>}
      <h1 id={titleId}>{title}</h1>
      <p>{message}</p>
      {actions && <div className="ui-empty-state__actions">{actions}</div>}
    </section>
  )
}
