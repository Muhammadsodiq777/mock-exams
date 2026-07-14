import type { HTMLAttributes } from 'react'

import { cx } from './shared'

type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone
}

export function Badge({ children, className, tone = 'neutral', ...props }: BadgeProps) {
  return (
    <span {...props} className={cx('ui-badge', `ui-badge--${tone}`, className)}>
      {children}
    </span>
  )
}
