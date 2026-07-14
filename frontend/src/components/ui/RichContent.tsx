import type { ReactNode } from 'react'

import { cx } from './shared'

export type RichContentBlock =
  | { text: string; type: 'paragraph' }
  | { level?: 2 | 3 | 4; text: string; type: 'heading' }
  | { items: string[]; type: 'list' }

export type RichContentProps = {
  blocks?: RichContentBlock[]
  children?: ReactNode
  className?: string
  trustedHtml?: string
}

export function RichContent({ blocks, children, className, trustedHtml }: RichContentProps) {
  if (trustedHtml) {
    return (
      <div
        className={cx('ui-rich-content', className)}
        dangerouslySetInnerHTML={{ __html: trustedHtml }}
      />
    )
  }

  if (blocks) {
    return (
      <div className={cx('ui-rich-content', className)}>
        {blocks.map((block, index) => {
          if (block.type === 'heading') {
            const Heading = `h${block.level ?? 3}` as 'h2' | 'h3' | 'h4'
            return <Heading key={index}>{block.text}</Heading>
          }

          if (block.type === 'list') {
            return (
              <ul key={index}>
                {block.items.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            )
          }

          return <p key={index}>{block.text}</p>
        })}
      </div>
    )
  }

  return <div className={cx('ui-rich-content', className)}>{children}</div>
}
