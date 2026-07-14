import type { ReactNode } from 'react'

import { cx } from './shared'

export type SplitPaneProps = {
  left: ReactNode
  right: ReactNode
  leftLabel?: string
  rightLabel?: string
}

export function SplitPane({ left, leftLabel = 'Source', right, rightLabel = 'Workspace' }: SplitPaneProps) {
  return (
    <div className="ui-split-pane">
      <section aria-label={leftLabel} className="ui-split-pane__pane">
        {left}
      </section>
      <section aria-label={rightLabel} className="ui-split-pane__pane">
        {right}
      </section>
    </div>
  )
}

export type PlayerLayoutProps = {
  children: ReactNode
  footer?: ReactNode
  sidebar?: ReactNode
}

export function PlayerLayout({ children, footer, sidebar }: PlayerLayoutProps) {
  return (
    <div className={cx('ui-player-layout', Boolean(sidebar) && 'ui-player-layout--with-sidebar')}>
      {sidebar && <aside className="ui-player-layout__sidebar">{sidebar}</aside>}
      <div className="ui-player-layout__main">{children}</div>
      {footer && <div className="ui-player-layout__footer">{footer}</div>}
    </div>
  )
}
