import type {
  AiFeedbackStatus,
  AiUsageRemainingRDO,
  ApiErrorCode,
  ApiErrorEnvelope,
  ApiFieldError,
  ApiListMeta,
  ApiListResponse,
  ApiSuccessEnvelope,
  ApiSuccessMeta,
  CommandResponseRDO,
  CommandStatus,
  PageMetadata,
  RichTextContentRDO,
  RichTextDocument,
  WritingFeedbackRDO,
} from '../types/backend'

export const TEST_REQUEST_ID = 'req_test_0001'

export const ASYNC_AI_FEEDBACK_STATUSES = [
  'queued',
  'processing',
  'completed',
  'failed',
] as const satisfies readonly AiFeedbackStatus[]

export const PENDING_AI_FEEDBACK_STATUSES = [
  'queued',
  'processing',
] as const satisfies readonly AiFeedbackStatus[]

export const TERMINAL_AI_FEEDBACK_STATUSES = [
  'completed',
  'failed',
] as const satisfies readonly AiFeedbackStatus[]

export function createApiMeta(overrides: Partial<ApiSuccessMeta> = {}): ApiSuccessMeta {
  return {
    requestId: overrides.requestId ?? TEST_REQUEST_ID,
  }
}

export function createSuccessEnvelope<TData>(
  data: TData,
  meta: Partial<ApiSuccessMeta> = {},
): ApiSuccessEnvelope<TData> {
  return {
    data,
    meta: createApiMeta(meta),
  }
}

export function createPageMetadata(
  itemCount = 0,
  overrides: Partial<PageMetadata> = {},
): PageMetadata {
  const size = overrides.size ?? Math.max(itemCount, 1)
  const number = overrides.number ?? 0
  const totalElements = overrides.totalElements ?? itemCount
  const totalPages = overrides.totalPages ?? (totalElements === 0 ? 0 : Math.ceil(totalElements / size))

  return {
    number,
    size,
    totalElements,
    totalPages,
    first: overrides.first ?? (number === 0),
    last: overrides.last ?? (totalPages === 0 || number + 1 >= totalPages),
  }
}

type ListMetaOverrides = {
  filters?: ApiListMeta['filters']
  page?: Partial<PageMetadata>
  requestId?: string
  sort?: ApiListMeta['sort']
}

export function createListMeta(
  itemCount = 0,
  overrides: ListMetaOverrides = {},
): ApiListMeta {
  return {
    requestId: overrides.requestId ?? TEST_REQUEST_ID,
    page: createPageMetadata(itemCount, overrides.page),
    sort: overrides.sort,
    filters: overrides.filters,
  }
}

export function createListEnvelope<TItem>(
  items: TItem[],
  meta: ListMetaOverrides = {},
): ApiListResponse<TItem> {
  return {
    data: items,
    meta: createListMeta(items.length, meta),
  }
}

export function createCommandResponse(
  status: CommandStatus = 'completed',
  overrides: Partial<CommandResponseRDO> = {},
): CommandResponseRDO {
  return {
    status,
    message: overrides.message ?? null,
    resourceId: overrides.resourceId ?? null,
    jobId: overrides.jobId ?? null,
  }
}

type ErrorEnvelopeOptions<TCode extends string> = {
  code: TCode
  details?: Record<string, unknown>
  fieldErrors?: ApiFieldError[]
  message?: string
  requestId?: string
}

export function createApiErrorEnvelope<TCode extends string = ApiErrorCode>({
  code,
  details,
  fieldErrors,
  message = 'The request could not be completed.',
  requestId = TEST_REQUEST_ID,
}: ErrorEnvelopeOptions<TCode>): ApiErrorEnvelope<TCode> {
  return {
    error: {
      code,
      message,
      fieldErrors,
      details,
      requestId,
    },
  }
}

export function createFieldError(
  overrides: Partial<ApiFieldError> & Pick<ApiFieldError, 'field'>,
): ApiFieldError {
  return {
    field: overrides.field,
    code: overrides.code ?? 'NotBlank',
    message: overrides.message ?? 'This field is required.',
  }
}

export function createValidationErrorEnvelope(
  fieldErrors: ApiFieldError[] = [createFieldError({ field: 'title' })],
  message = 'Validation failed.',
): ApiErrorEnvelope<'VALIDATION_FAILED'> {
  return createApiErrorEnvelope({
    code: 'VALIDATION_FAILED',
    message,
    fieldErrors,
  })
}

export function createRichTextDocument(
  paragraphs: readonly string[] = ['Sample paragraph.'],
): RichTextDocument {
  return {
    version: 1,
    type: 'doc',
    content: paragraphs
      .map((paragraph) => paragraph.trim())
      .filter(Boolean)
      .map((paragraph) => ({
        type: 'paragraph',
        content: [{ type: 'text', text: paragraph }],
      })),
  }
}

const htmlEscapes: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
}

const escapeHtml = (value: string) => value.replace(/[&<>"']/g, (match) => htmlEscapes[match])

export function createRichTextContent(
  paragraphs: readonly string[] = ['Sample paragraph.'],
  overrides: Partial<RichTextContentRDO> = {},
): RichTextContentRDO {
  const cleanParagraphs = paragraphs.map((paragraph) => paragraph.trim()).filter(Boolean)
  const renderedHtml = cleanParagraphs
    .map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`)
    .join('')

  return {
    contentDocument: createRichTextDocument(cleanParagraphs),
    contentText: cleanParagraphs.join('\n\n'),
    renderedHtml,
    ...overrides,
  }
}

export function createAiUsageRemainingFixture(
  overrides: Partial<AiUsageRemainingRDO> = {},
): AiUsageRemainingRDO {
  return {
    periodStart: '2026-01-01',
    periodEnd: '2026-01-31',
    freeRequestsLimit: 20,
    usedRequests: 4,
    hardLimitRequests: 40,
    freeRequestsRemaining: 16,
    hardRequestsRemaining: 36,
    hardLimitReached: false,
    ...overrides,
  }
}

export function createWritingFeedbackFixture(
  status: AiFeedbackStatus = 'completed',
  overrides: Partial<WritingFeedbackRDO> = {},
): WritingFeedbackRDO {
  return {
    id: 4001,
    submissionId: 3001,
    feedbackType: 'writing',
    status,
    overallBand: status === 'completed' ? 7 : null,
    criteriaScores: {
      taskAchievement: {
        band: status === 'completed' ? 7 : null,
        feedback: status === 'completed' ? 'Addresses the task with clear detail.' : '',
        suggestions: status === 'completed' ? ['Add one more concrete example.'] : [],
      },
    },
    mistakes: [],
    improvedAnswer: status === 'completed' ? 'Improved sample answer.' : null,
    actionPlan: status === 'completed' ? ['Review paragraph cohesion.'] : [],
    failureCode: status === 'failed' ? 'AI_PROVIDER_FAILED' : null,
    failureMessage: status === 'failed' ? 'The provider could not complete feedback.' : null,
    provider: status === 'completed' ? 'mock-provider' : null,
    model: status === 'completed' ? 'mock-model' : null,
    promptTokens: status === 'completed' ? 250 : 0,
    completionTokens: status === 'completed' ? 180 : 0,
    estimatedCost: status === 'completed' ? 0.03 : 0,
    rubricVersion: 'ielts-writing-v1',
    retryCount: 0,
    retryOfFeedbackId: null,
    createdAt: '2026-01-15T10:00:00Z',
    completedAt: status === 'completed' ? '2026-01-15T10:00:20Z' : null,
    ...overrides,
  }
}
