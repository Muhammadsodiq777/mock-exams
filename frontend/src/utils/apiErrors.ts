import axios from 'axios'

import type { ApiErrorCode, ApiErrorEnvelope, ApiFieldError } from '../types/backend'

const sessionExpiryCodes = new Set<ApiErrorCode>([
  'AUTHENTICATION_REQUIRED',
  'TOKEN_INVALID',
  'TOKEN_EXPIRED',
  'REFRESH_TOKEN_INVALID',
])

const accountInactiveCode: ApiErrorCode = 'ACCOUNT_INACTIVE'

export type FormErrorState<TField extends string> = {
  formMessage: string | null
  fieldErrors: Partial<Record<TField, string>>
}

export type NormalizedApiError = {
  code: ApiErrorCode | string
  message: string
  fieldErrors: ApiFieldError[]
  details: Record<string, unknown>
  requestId: string | null
  status: number | null
}

const fallbackApiErrorMessage = 'Unable to complete the request.'

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const isApiFieldError = (value: unknown): value is ApiFieldError => {
  if (!isRecord(value)) {
    return false
  }

  return (
    typeof value.field === 'string' &&
    typeof value.code === 'string' &&
    typeof value.message === 'string'
  )
}

const isApiErrorEnvelope = (value: unknown): value is ApiErrorEnvelope => {
  if (!isRecord(value) || !isRecord(value.error)) {
    return false
  }

  const { error } = value

  return (
    typeof error.code === 'string' &&
    typeof error.message === 'string' &&
    typeof error.requestId === 'string' &&
    (error.fieldErrors === undefined ||
      (Array.isArray(error.fieldErrors) && error.fieldErrors.every(isApiFieldError))) &&
    (error.details === undefined || isRecord(error.details))
  )
}

export function getApiError(error: unknown): ApiErrorEnvelope['error'] | null {
  if (!axios.isAxiosError<ApiErrorEnvelope>(error)) {
    return null
  }

  const data = error.response?.data

  return isApiErrorEnvelope(data) ? data.error : null
}

export function normalizeApiError(error: unknown): NormalizedApiError {
  const apiError = getApiError(error)
  const status = axios.isAxiosError(error) ? error.response?.status ?? null : null

  if (!apiError) {
    return {
      code: 'INTERNAL_ERROR',
      message: error instanceof Error ? error.message : fallbackApiErrorMessage,
      fieldErrors: [],
      details: {},
      requestId: null,
      status,
    }
  }

  return {
    code: apiError.code,
    message: apiError.message,
    fieldErrors: apiError.fieldErrors ?? [],
    details: apiError.details ?? {},
    requestId: apiError.requestId,
    status,
  }
}

export function getApiErrorMessage(error: unknown, fallbackMessage: string) {
  return getApiError(error)?.message ?? fallbackMessage
}

export function isSessionExpiryError(error: unknown) {
  if (!axios.isAxiosError<ApiErrorEnvelope>(error)) {
    return false
  }

  const status = error.response?.status
  const code = getApiError(error)?.code

  return status === 401 && Boolean(code && sessionExpiryCodes.has(code))
}

export function isAccountInactiveError(error: unknown) {
  if (!axios.isAxiosError<ApiErrorEnvelope>(error)) {
    return false
  }

  const status = error.response?.status
  const code = getApiError(error)?.code

  return status === 403 && code === accountInactiveCode
}

export function mapFieldErrors<TField extends string>(
  fieldErrors: ApiFieldError[] | undefined,
  supportedFields: readonly TField[],
) {
  const supportedFieldSet = new Set<string>(supportedFields)
  const mappedErrors: Partial<Record<TField, string>> = {}

  fieldErrors?.forEach((fieldError) => {
    if (supportedFieldSet.has(fieldError.field)) {
      mappedErrors[fieldError.field as TField] = fieldError.message
    }
  })

  return mappedErrors
}

export function toFormErrorState<TField extends string>(
  error: unknown,
  supportedFields: readonly TField[],
  fallbackMessage: string,
): FormErrorState<TField> {
  const apiError = getApiError(error)

  return {
    formMessage: apiError?.message ?? fallbackMessage,
    fieldErrors: mapFieldErrors(apiError?.fieldErrors, supportedFields),
  }
}
