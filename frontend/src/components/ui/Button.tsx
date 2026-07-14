import type { ButtonHTMLAttributes, ReactNode } from 'react'

import { cx } from './shared'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md'

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: ReactNode
  isLoading?: boolean
  loadingLabel?: string
  size?: ButtonSize
  variant?: ButtonVariant
}

export function Button({
  children,
  className,
  disabled,
  icon,
  isLoading = false,
  loadingLabel,
  size = 'md',
  type = 'button',
  variant = 'primary',
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      aria-busy={isLoading || undefined}
      className={cx('ui-button', `ui-button--${variant}`, `ui-button--${size}`, className)}
      disabled={disabled || isLoading}
      type={type}
    >
      {isLoading && <span className="ui-button__spinner" aria-hidden="true" />}
      {!isLoading && icon && <span className="ui-button__icon">{icon}</span>}
      <span className="ui-button__label">{isLoading && loadingLabel ? loadingLabel : children}</span>
    </button>
  )
}
