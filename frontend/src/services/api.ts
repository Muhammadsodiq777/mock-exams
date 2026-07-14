import axios from 'axios'
import type { AxiosResponse } from 'axios'

import { useSessionStore } from '../store/sessionStore'
import type {
  ApiCommandResponse,
  ApiCommandResult,
  ApiDashboardResponse,
  ApiDashboardResult,
  ApiErrorEnvelope,
  ApiListMeta,
  ApiListResponse,
  ApiListResult,
  ApiSuccessEnvelope,
  ApiSuccessMeta,
  ApiUnpagedListResponse,
  ApiUnpagedListResult,
} from '../types/backend'

const defaultApiBaseUrl = '/api/v1'
const sessionExpiryCodes = new Set<string>([
  'AUTHENTICATION_REQUIRED',
  'TOKEN_INVALID',
  'TOKEN_EXPIRED',
  'REFRESH_TOKEN_INVALID',
])

const trimTrailingSlashes = (value: string) => (value === '/' ? value : value.replace(/\/+$/, ''))

const resolveApiBaseUrl = (rawValue: string | undefined) => {
  const value = rawValue?.trim()

  if (!value) {
    return defaultApiBaseUrl
  }

  if (value.startsWith('//')) {
    throw new Error('VITE_API_BASE_URL must not be protocol-relative. Use http://, https://, or a root-relative path.')
  }

  if (value.startsWith('/')) {
    return trimTrailingSlashes(value)
  }

  try {
    return trimTrailingSlashes(new URL(value).toString())
  } catch {
    throw new Error('VITE_API_BASE_URL must be an absolute URL or a root-relative path, for example /api/v1.')
  }
}

const readFeatureFlag = (name: string, rawValue: string | undefined) => {
  const value = rawValue?.trim().toLowerCase()

  if (!value) {
    return false
  }

  if (['true', '1', 'yes', 'on'].includes(value)) {
    return true
  }

  if (['false', '0', 'no', 'off'].includes(value)) {
    return false
  }

  throw new Error(`${name} must be one of true, false, 1, 0, yes, no, on, or off.`)
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const getApiErrorCode = (data: unknown) => {
  if (!isRecord(data) || !isRecord(data.error)) {
    return null
  }

  return typeof data.error.code === 'string' ? data.error.code : null
}

export const apiBaseUrl = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL)

export const featureFlags = Object.freeze({
  auth: readFeatureFlag('VITE_FEATURE_AUTH', import.meta.env.VITE_FEATURE_AUTH),
  ai: readFeatureFlag('VITE_FEATURE_AI', import.meta.env.VITE_FEATURE_AI),
  payments: readFeatureFlag('VITE_FEATURE_PAYMENTS', import.meta.env.VITE_FEATURE_PAYMENTS),
  marketplace: readFeatureFlag('VITE_FEATURE_MARKETPLACE', import.meta.env.VITE_FEATURE_MARKETPLACE),
})

export const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
})

export function unwrapResourceResponse<
  TData,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
>(response: AxiosResponse<ApiSuccessEnvelope<TData, TMeta>>) {
  return {
    data: response.data.data,
    meta: response.data.meta,
  }
}

export function unwrapResource<
  TData,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
>(response: AxiosResponse<ApiSuccessEnvelope<TData, TMeta>>) {
  return unwrapResourceResponse(response).data
}

export function unwrapListResponse<
  TItem,
  TMeta extends ApiListMeta = ApiListMeta,
>(response: AxiosResponse<ApiListResponse<TItem, TMeta>>): ApiListResult<TItem, TMeta> {
  return {
    items: response.data.data,
    meta: response.data.meta,
  }
}

export const unwrapPageResponse = unwrapListResponse

export function unwrapUnpagedListResponse<
  TItem,
  TMeta extends ApiSuccessMeta = ApiSuccessMeta,
>(response: AxiosResponse<ApiUnpagedListResponse<TItem, TMeta>>): ApiUnpagedListResult<TItem, TMeta> {
  return {
    items: response.data.data,
    meta: response.data.meta,
  }
}

export function unwrapDashboardResponse<TDashboard>(
  response: AxiosResponse<ApiDashboardResponse<TDashboard>>,
): ApiDashboardResult<TDashboard> {
  return {
    dashboard: response.data.data,
    meta: response.data.meta,
  }
}

export function unwrapCommandResponse<TData = null>(
  response: AxiosResponse<ApiCommandResponse<TData>>,
): ApiCommandResult<TData> {
  return {
    data: response.data.data,
    meta: response.data.meta,
  }
}

api.interceptors.request.use((config) => {
  const { accessToken } = useSessionStore.getState()

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError<ApiErrorEnvelope>(error)) {
      const status = error.response?.status
      const code = getApiErrorCode(error.response?.data)

      if (status === 401 && code && sessionExpiryCodes.has(code)) {
        useSessionStore.getState().clearSession()
      }
    }

    return Promise.reject(error)
  },
)
