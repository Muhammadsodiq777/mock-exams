import {
  type KeyboardEvent,
  type ReactNode,
  type RefObject,
  useEffect,
  useId,
  useRef,
} from 'react'
import { createPortal } from 'react-dom'

import { Button } from './Button'

const focusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'textarea:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export type DialogProps = {
  children: ReactNode
  closeLabel?: string
  description?: string
  footer?: ReactNode
  initialFocusRef?: RefObject<HTMLElement>
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  title: string
}

export function Dialog({
  children,
  closeLabel = 'Close',
  description,
  footer,
  initialFocusRef,
  isOpen,
  onOpenChange,
  title,
}: DialogProps) {
  const titleId = useId()
  const descriptionId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const focusTarget =
      initialFocusRef?.current ??
      dialogRef.current?.querySelector<HTMLElement>(focusableSelector) ??
      dialogRef.current

    focusTarget?.focus()
    document.body.classList.add('ui-dialog-open')

    return () => {
      document.body.classList.remove('ui-dialog-open')
      previousFocusRef.current?.focus()
    }
  }, [initialFocusRef, isOpen])

  if (!isOpen) {
    return null
  }

  const closeDialog = () => onOpenChange(false)

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault()
      closeDialog()
      return
    }

    if (event.key !== 'Tab' || !dialogRef.current) {
      return
    }

    const focusableElements = Array.from(
      dialogRef.current.querySelectorAll<HTMLElement>(focusableSelector),
    )

    if (focusableElements.length === 0) {
      event.preventDefault()
      dialogRef.current.focus()
      return
    }

    const first = focusableElements[0]
    const last = focusableElements[focusableElements.length - 1]

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return createPortal(
    <div className="ui-dialog-backdrop" onMouseDown={closeDialog}>
      <div
        aria-describedby={description ? descriptionId : undefined}
        aria-labelledby={titleId}
        aria-modal="true"
        className="ui-dialog"
        onKeyDown={handleKeyDown}
        onMouseDown={(event) => event.stopPropagation()}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
      >
        <div className="ui-dialog__header">
          <div>
            <h2 id={titleId}>{title}</h2>
            {description && <p id={descriptionId}>{description}</p>}
          </div>
          <Button aria-label={closeLabel} onClick={closeDialog} size="sm" variant="ghost">
            x
          </Button>
        </div>
        <div className="ui-dialog__body">{children}</div>
        {footer && <div className="ui-dialog__footer">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}
