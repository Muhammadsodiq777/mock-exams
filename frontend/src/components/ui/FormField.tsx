import {
  forwardRef,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
  useId,
} from 'react'

import { cx } from './shared'

type FieldChromeProps = {
  children: (ids: { describedBy?: string; fieldId: string; invalid: boolean }) => ReactNode
  className?: string
  error?: string
  helpText?: string
  id?: string
  isLoading?: boolean
  label: string
  required?: boolean
}

function FieldChrome({
  children,
  className,
  error,
  helpText,
  id,
  isLoading = false,
  label,
  required,
}: FieldChromeProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  const helpId = helpText ? `${fieldId}-help` : undefined
  const errorId = error ? `${fieldId}-error` : undefined
  const describedBy = [helpId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className={cx('ui-field', error && 'ui-field--error', className)}>
      <label className="ui-field__label" htmlFor={fieldId}>
        <span>{label}</span>
        {required && <span aria-hidden="true">*</span>}
        {isLoading && <span className="ui-field__status">Loading</span>}
      </label>
      {children({ describedBy, fieldId, invalid: Boolean(error) })}
      {helpText && (
        <p className="ui-field__help" id={helpId}>
          {helpText}
        </p>
      )}
      {error && (
        <p className="ui-field__error" id={errorId} role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  error?: string
  helpText?: string
  id?: string
  isLoading?: boolean
  label: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { className, disabled, error, helpText, id, isLoading, label, required, ...props },
  ref,
) {
  return (
    <FieldChrome
      className={className}
      error={error}
      helpText={helpText}
      id={id}
      isLoading={isLoading}
      label={label}
      required={required}
    >
      {({ describedBy, fieldId, invalid }) => (
        <input
          {...props}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className="ui-input"
          disabled={disabled || isLoading}
          id={fieldId}
          ref={ref}
          required={required}
        />
      )}
    </FieldChrome>
  )
})

export type SelectFieldProps = Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> & {
  error?: string
  helpText?: string
  id?: string
  isLoading?: boolean
  label: string
}

export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(function SelectField(
  { children, className, disabled, error, helpText, id, isLoading, label, required, ...props },
  ref,
) {
  return (
    <FieldChrome
      className={className}
      error={error}
      helpText={helpText}
      id={id}
      isLoading={isLoading}
      label={label}
      required={required}
    >
      {({ describedBy, fieldId, invalid }) => (
        <select
          {...props}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className="ui-input"
          disabled={disabled || isLoading}
          id={fieldId}
          ref={ref}
          required={required}
        >
          {children}
        </select>
      )}
    </FieldChrome>
  )
})

export type TextareaFieldProps = Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'> & {
  error?: string
  helpText?: string
  id?: string
  isLoading?: boolean
  label: string
}

export const TextareaField = forwardRef<HTMLTextAreaElement, TextareaFieldProps>(
  function TextareaField(
    { className, disabled, error, helpText, id, isLoading, label, required, ...props },
    ref,
  ) {
    return (
      <FieldChrome
        className={className}
        error={error}
        helpText={helpText}
        id={id}
        isLoading={isLoading}
        label={label}
        required={required}
      >
        {({ describedBy, fieldId, invalid }) => (
          <textarea
            {...props}
            aria-describedby={describedBy}
            aria-invalid={invalid || undefined}
            className="ui-input ui-input--textarea"
            disabled={disabled || isLoading}
            id={fieldId}
            ref={ref}
            required={required}
          />
        )}
      </FieldChrome>
    )
  },
)
