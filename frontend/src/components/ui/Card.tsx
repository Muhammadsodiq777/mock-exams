import type { ReactNode } from 'react'

import { cx } from './shared'

export type RepeatedItemCardProps = {
  children: ReactNode
  className?: string
}

export function RepeatedItemCard({ children, className }: RepeatedItemCardProps) {
  return <article className={cx('ui-item-card', className)}>{children}</article>
}

export type DescriptionListProps = {
  items: Array<{ label: ReactNode; value: ReactNode }>
}

export function DescriptionList({ items }: DescriptionListProps) {
  return (
    <dl className="ui-description-list">
      {items.map((item, index) => (
        <div key={index}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}
