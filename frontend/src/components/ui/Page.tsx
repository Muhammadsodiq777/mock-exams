import type { ReactNode } from 'react'

import { cx } from './shared'

export type PageContainerProps = {
  children: ReactNode
  className?: string
  size?: 'compact' | 'default' | 'wide'
}

export function PageContainer({ children, className, size = 'default' }: PageContainerProps) {
  return <main className={cx('ui-page', `ui-page--${size}`, className)}>{children}</main>
}

export type PageHeaderProps = {
  actions?: ReactNode
  className?: string
  eyebrow?: string
  title: string
}

export function PageHeader({ actions, className, eyebrow, title }: PageHeaderProps) {
  return (
    <section className={cx('ui-page-header', className)} aria-label="Overview">
      <div className="ui-page-header__copy">
        {eyebrow && <p className="ui-eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
      </div>
      {actions && <div className="ui-page-header__actions">{actions}</div>}
    </section>
  )
}

export type PanelProps = {
  children: ReactNode
  className?: string
  title?: string
  description?: string
}

export function Panel({ children, className, description, title }: PanelProps) {
  return (
    <section className={cx('ui-panel', className)} aria-label={title}>
      {(title || description) && (
        <div className="ui-panel__header">
          <div>
            {title && <h2>{title}</h2>}
            {description && <p>{description}</p>}
          </div>
        </div>
      )}
      {children}
    </section>
  )
}
