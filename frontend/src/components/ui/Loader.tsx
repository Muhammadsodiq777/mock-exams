import { cx } from './shared'

export type LoaderProps = {
  className?: string
  label?: string
  size?: 'sm' | 'md'
}

export function Loader({ className, label = 'Loading', size = 'md' }: LoaderProps) {
  return (
    <span className={cx('ui-loader', `ui-loader--${size}`, className)} role="status">
      <span className="ui-loader__spinner" aria-hidden="true" />
      <span>{label}</span>
    </span>
  )
}

export type SkeletonProps = {
  className?: string
  lines?: number
}

export function Skeleton({ className, lines = 3 }: SkeletonProps) {
  return (
    <div className={cx('ui-skeleton', className)} aria-hidden="true">
      {Array.from({ length: lines }, (_, index) => (
        <span className="ui-skeleton__line" key={index} />
      ))}
    </div>
  )
}
