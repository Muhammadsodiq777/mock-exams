import type { ReactNode } from 'react'

import { cx } from './shared'

type AlertVariant = 'info' | 'success' | 'warning' | 'danger'

export type AlertProps = {
  children: ReactNode
  className?: string
  title?: string
  variant?: AlertVariant
}

export function Alert({ children, className, title, variant = 'info' }: AlertProps) {
  return (
    <div
      className={cx('ui-alert', `ui-alert--${variant}`, className)}
      role={variant === 'danger' ? 'alert' : 'status'}
    >
      {title && <p className="ui-alert__title">{title}</p>}
      <div className="ui-alert__body">{children}</div>
    </div>
  )
}
