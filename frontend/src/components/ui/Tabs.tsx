import { type KeyboardEvent, type ReactNode, useId, useState } from 'react'

import { cx } from './shared'

export type TabItem = {
  content: ReactNode
  disabled?: boolean
  id: string
  label: string
}

export type TabsProps = {
  className?: string
  defaultValue?: string
  items: TabItem[]
  onValueChange?: (value: string) => void
  value?: string
}

export function Tabs({ className, defaultValue, items, onValueChange, value }: TabsProps) {
  const generatedId = useId()
  const firstEnabled = items.find((item) => !item.disabled)?.id
  const [internalValue, setInternalValue] = useState(defaultValue ?? firstEnabled)
  const activeValue = value ?? internalValue
  const activeItem = items.find((item) => item.id === activeValue) ?? items[0]

  const selectTab = (nextValue: string) => {
    setInternalValue(nextValue)
    onValueChange?.(nextValue)
  }

  const focusTab = (nextIndex: number) => {
    const enabledItems = items.filter((item) => !item.disabled)
    const nextItem = enabledItems[nextIndex]

    if (!nextItem) {
      return
    }

    selectTab(nextItem.id)
    document.getElementById(`${generatedId}-${nextItem.id}-tab`)?.focus()
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>, itemId: string) => {
    const enabledItems = items.filter((item) => !item.disabled)
    const currentIndex = enabledItems.findIndex((item) => item.id === itemId)

    if (currentIndex < 0) {
      return
    }

    if (event.key === 'ArrowRight') {
      event.preventDefault()
      focusTab((currentIndex + 1) % enabledItems.length)
    } else if (event.key === 'ArrowLeft') {
      event.preventDefault()
      focusTab((currentIndex - 1 + enabledItems.length) % enabledItems.length)
    } else if (event.key === 'Home') {
      event.preventDefault()
      focusTab(0)
    } else if (event.key === 'End') {
      event.preventDefault()
      focusTab(enabledItems.length - 1)
    }
  }

  return (
    <div className={cx('ui-tabs', className)}>
      <div className="ui-tabs__list" role="tablist">
        {items.map((item) => {
          const selected = item.id === activeItem?.id
          const tabId = `${generatedId}-${item.id}-tab`
          const panelId = `${generatedId}-${item.id}-panel`

          return (
            <button
              aria-controls={panelId}
              aria-selected={selected}
              className={cx('ui-tabs__tab', selected && 'active')}
              disabled={item.disabled}
              id={tabId}
              key={item.id}
              onKeyDown={(event) => handleKeyDown(event, item.id)}
              onClick={() => selectTab(item.id)}
              role="tab"
              type="button"
            >
              {item.label}
            </button>
          )
        })}
      </div>
      {activeItem && (
        <div
          aria-labelledby={`${generatedId}-${activeItem.id}-tab`}
          className="ui-tabs__panel"
          id={`${generatedId}-${activeItem.id}-panel`}
          role="tabpanel"
        >
          {activeItem.content}
        </div>
      )}
    </div>
  )
}
