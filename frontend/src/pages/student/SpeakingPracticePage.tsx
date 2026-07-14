import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  DescriptionList,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  SelectField,
  TextareaField,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import { useAiUsage } from '../../hooks/useStudentProgress'
import {
  isExternalCheckoutUrl,
  useCancelLiveSpeakingBooking,
  useCreateLiveSpeakingBookingPayment,
  usePublicScheduleSlots,
  useReserveLiveSpeakingSlot,
  useStudentScheduleBookings,
} from '../../hooks/useLiveSpeakingSchedule'
import {
  useCreateSpeakingSubmission,
  useRetrySpeakingFeedback,
  useSpeakingFeedback,
  useSpeakingSubmission,
  useStartSpeakingFeedback,
  useStartSpeakingTranscription,
  useUploadSpeakingAudio,
} from '../../hooks/useSpeakingPractice'
import { featureFlags } from '../../services/api'
import type {
  BackendId,
  SpeakingCriterionFeedbackRDO,
  SpeakingFeedbackRDO,
  LiveSpeakingBookingRDO,
  LiveSpeakingSlotRDO,
  SpeakingPartType,
  SpeakingSubmissionRDO,
} from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatCurrency, formatDate, formatNumber, formatSeconds, humanizeToken } from '../../utils/formatters'

type SpeakingDraft = {
  audioContentType?: string | null
  audioFileId?: BackendId | null
  audioFileName?: string | null
  audioSizeBytes?: number | null
  durationSeconds?: number | null
  partType: SpeakingPartType
  prompt: string
  submissionId?: BackendId | null
}

type SpeakingField = 'partType' | 'prompt' | 'audioFileId'

type RecorderState = 'idle' | 'recording' | 'stopping'

const storageKey = 'mock-exams.speaking-practice'
const maxAudioBytes = 25 * 1024 * 1024
const fieldNames = ['partType', 'prompt', 'audioFileId'] as const

const defaultDraft: SpeakingDraft = {
  audioContentType: null,
  audioFileId: null,
  audioFileName: null,
  audioSizeBytes: null,
  durationSeconds: null,
  partType: 'speaking_part_2',
  prompt: '',
  submissionId: null,
}

const allowedAudioTypes = new Set([
  'audio/aac',
  'audio/m4a',
  'audio/mp3',
  'audio/mp4',
  'audio/mpeg',
  'audio/ogg',
  'audio/wav',
  'audio/webm',
  'audio/x-m4a',
  'audio/x-wav',
])

const extensionTypeMap: Record<string, string> = {
  aac: 'audio/aac',
  m4a: 'audio/x-m4a',
  mp3: 'audio/mpeg',
  mp4: 'audio/mp4',
  oga: 'audio/ogg',
  ogg: 'audio/ogg',
  wav: 'audio/wav',
  webm: 'audio/webm',
}

const partGuidance: Record<SpeakingPartType, { label: string; prompt: string; time: string }> = {
  speaking_part_1: {
    label: 'Part 1',
    prompt: 'Answer short personal questions. Aim for clear, natural answers with a little detail.',
    time: '30-45 seconds',
  },
  speaking_part_2: {
    label: 'Part 2',
    prompt: 'Speak from a cue card. Build a sustained answer with examples and a closing idea.',
    time: '1-2 minutes',
  },
  speaking_part_3: {
    label: 'Part 3',
    prompt: 'Discuss abstract follow-up questions. Give reasons, contrasts, and examples.',
    time: '45-90 seconds',
  },
}

const criteriaOrder = [
  'fluency_and_coherence',
  'lexical_resource',
  'grammatical_range_and_accuracy',
  'pronunciation_proxy',
] as const

const loadDraft = (): SpeakingDraft => {
  if (typeof window === 'undefined') {
    return defaultDraft
  }

  const rawValue = window.localStorage.getItem(storageKey)
  if (!rawValue) {
    return defaultDraft
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<SpeakingDraft>
    return {
      audioContentType: parsed.audioContentType ?? null,
      audioFileId: typeof parsed.audioFileId === 'number' ? parsed.audioFileId : null,
      audioFileName: parsed.audioFileName ?? null,
      audioSizeBytes: typeof parsed.audioSizeBytes === 'number' ? parsed.audioSizeBytes : null,
      durationSeconds:
        typeof parsed.durationSeconds === 'number' && parsed.durationSeconds > 0
          ? parsed.durationSeconds
          : null,
      partType:
        parsed.partType === 'speaking_part_1' ||
        parsed.partType === 'speaking_part_2' ||
        parsed.partType === 'speaking_part_3'
          ? parsed.partType
          : defaultDraft.partType,
      prompt: parsed.prompt ?? '',
      submissionId: typeof parsed.submissionId === 'number' ? parsed.submissionId : null,
    }
  } catch {
    window.localStorage.removeItem(storageKey)
    return defaultDraft
  }
}

const saveDraft = (draft: SpeakingDraft) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(storageKey, JSON.stringify(draft))
  }
}

const fileExtension = (name: string) => name.split('.').pop()?.toLowerCase() ?? ''

const inferredContentType = (file: File) => {
  if (allowedAudioTypes.has(file.type)) {
    return file.type
  }

  return extensionTypeMap[fileExtension(file.name)] ?? ''
}

const normalizeAudioFile = (file: File) => {
  const contentType = inferredContentType(file)
  return contentType && file.type !== contentType ? new File([file], file.name, { type: contentType }) : file
}

const validateAudioFile = (file: File) => {
  if (file.size <= 0) {
    return 'Choose a non-empty audio file.'
  }

  if (file.size > maxAudioBytes) {
    return 'Speaking audio must be 25 MiB or smaller.'
  }

  if (!allowedAudioTypes.has(inferredContentType(file))) {
    return 'Use AAC, M4A, MP3, MP4, OGG, WAV, or WEBM audio.'
  }

  return null
}

const hasRecordingSupport = () =>
  typeof navigator !== 'undefined' &&
  Boolean(navigator.mediaDevices?.getUserMedia) &&
  typeof MediaRecorder !== 'undefined'

const preferredRecordingType = () => {
  if (typeof MediaRecorder === 'undefined') {
    return ''
  }

  return ['audio/webm', 'audio/mp4', 'audio/ogg'].find((type) => MediaRecorder.isTypeSupported(type)) ?? ''
}

const recordingExtension = (contentType: string) => {
  if (contentType.includes('mp4')) {
    return 'm4a'
  }

  if (contentType.includes('ogg')) {
    return 'ogg'
  }

  return 'webm'
}

const formatFileSize = (bytes?: number | null) => {
  if (!bytes) {
    return 'Not uploaded'
  }

  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const formatLocalDateTime = (value?: string | null, fallback = 'Not scheduled') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.valueOf())) {
    return fallback
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

const liveSpeakingStatusTone = (status: string) => {
  if (status === 'announced' || status === 'booked' || status === 'completed') {
    return 'success'
  }

  if (status === 'reserved' || status === 'draft' || status === 'pending') {
    return 'warning'
  }

  if (status === 'cancelled' || status === 'expired' || status === 'no_show' || status === 'failed') {
    return 'danger'
  }

  return 'neutral'
}

const draftMatchesSubmission = (draft: SpeakingDraft, submission?: SpeakingSubmissionRDO | null) =>
  Boolean(submission) &&
  submission?.prompt === draft.prompt &&
  submission?.partType === draft.partType &&
  submission?.audioFileId === draft.audioFileId

const criterionLabel = (key: string) =>
  key === 'pronunciation_proxy' ? 'Pronunciation proxy' : humanizeToken(key)

function CriteriaCard({
  criterion,
  label,
}: {
  criterion?: SpeakingCriterionFeedbackRDO
  label: string
}) {
  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone={criterion?.band ? 'success' : 'info'}>{label}</Badge>
        <Badge tone="info">Band {formatNumber(criterion?.band, '-')}</Badge>
      </div>
      <p className="feature-card__summary">
        {criterion?.feedback ?? 'Feedback is not available for this criterion yet.'}
      </p>
      {criterion?.suggestions?.length ? (
        <ul className="writing-list">
          {criterion.suggestions.map((suggestion) => (
            <li key={suggestion}>{suggestion}</li>
          ))}
        </ul>
      ) : null}
    </RepeatedItemCard>
  )
}

function FeedbackPanel({
  feedback,
  isFetching,
  onRefresh,
  onRetry,
  retrying,
}: {
  feedback?: SpeakingFeedbackRDO | null
  isFetching: boolean
  onRefresh: () => void
  onRetry: () => void
  retrying: boolean
}) {
  if (!feedback) {
    return (
      <EmptyState
        title="Feedback pending"
        message="Complete transcription to start AI speaking feedback."
      />
    )
  }

  if (feedback.status === 'queued' || feedback.status === 'processing') {
    return (
      <div className="feature-panel-state">
        <Loader label={feedback.status === 'queued' ? 'Feedback queued' : 'Feedback processing'} />
        <p className="feature-card__summary">
          The page will keep checking this feedback job. Refreshing does not create a duplicate request.
        </p>
        <Button isLoading={isFetching} loadingLabel="Refreshing" onClick={onRefresh} variant="secondary">
          Refresh feedback
        </Button>
      </div>
    )
  }

  if (feedback.status === 'failed') {
    return (
      <div className="feature-panel-state">
        <Alert title="Feedback failed" variant="danger">
          {feedback.failureMessage ??
            (feedback.failureCode ? humanizeToken(feedback.failureCode) : 'The AI provider could not complete feedback.')}
        </Alert>
        <div className="feature-card__actions">
          <Button isLoading={retrying} loadingLabel="Retrying" onClick={onRetry}>
            Retry feedback
          </Button>
          <Button isLoading={isFetching} loadingLabel="Refreshing" onClick={onRefresh} variant="secondary">
            Refresh
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="feature-stack">
      {feedback.transcriptOnlyPronunciation && (
        <Alert title="Pronunciation limitation" variant="warning">
          {feedback.pronunciationScoringNote}
        </Alert>
      )}

      <RepeatedItemCard>
        <div className="feature-card__badges">
          <Badge tone="success">Completed</Badge>
          <Badge tone="info">Band {formatNumber(feedback.overallBand)}</Badge>
        </div>
        <DescriptionList
          items={[
            { label: 'Completed', value: formatDate(feedback.completedAt ?? feedback.createdAt) },
            { label: 'Provider', value: feedback.provider ?? 'Not reported' },
            { label: 'Model', value: feedback.model ?? 'Not reported' },
            { label: 'Retry count', value: feedback.retryCount },
          ]}
        />
      </RepeatedItemCard>

      <div className="feature-card-grid feature-card-grid--compact">
        {criteriaOrder.map((key) => (
          <CriteriaCard
            criterion={feedback.criteriaScores[key]}
            key={key}
            label={criterionLabel(key)}
          />
        ))}
      </div>

      <Panel title="Repeated mistakes" description="Examples returned by the speaking feedback engine.">
        <div className="feature-panel-body">
          {feedback.repeatedMistakes.length ? (
            <div className="feature-list">
              {feedback.repeatedMistakes.map((mistake, index) => (
                <RepeatedItemCard key={`${mistake.type}-${mistake.original}-${index}`}>
                  <Badge tone="warning">{humanizeToken(mistake.type)}</Badge>
                  <DescriptionList
                    items={[
                      { label: 'Original', value: mistake.original },
                      { label: 'Correction', value: mistake.correction ?? 'Review manually' },
                      { label: 'Why', value: mistake.explanation ?? 'No explanation provided' },
                    ]}
                  />
                </RepeatedItemCard>
              ))}
            </div>
          ) : (
            <EmptyState title="No mistakes listed" message="The feedback did not return mistake examples." />
          )}
        </div>
      </Panel>

      <Panel title="Better answer" description="A stronger version of the spoken answer when available.">
        <div className="feature-panel-body">
          {feedback.betterAnswerExample ? (
            <pre className="writing-pre">{feedback.betterAnswerExample}</pre>
          ) : (
            <EmptyState title="No better answer" message="The feedback did not include an example." />
          )}
        </div>
      </Panel>

      <Panel title="Action plan" description="Short next steps from this feedback.">
        <div className="feature-panel-body">
          {feedback.actionPlan.length ? (
            <ol className="writing-list writing-list--numbered">
              {feedback.actionPlan.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ol>
          ) : (
            <EmptyState title="No action plan" message="The feedback did not include next steps." />
          )}
        </div>
      </Panel>
    </div>
  )
}

function StudentSlotCard({
  onMessage,
  slot,
}: {
  onMessage: (message: string) => void
  slot: LiveSpeakingSlotRDO
}) {
  const reserveSlot = useReserveLiveSpeakingSlot()

  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone="success">Available</Badge>
        <Badge tone="info">{formatCurrency(slot.priceAmount, slot.currency, 'Free')}</Badge>
      </div>
      <DescriptionList
        items={[
          { label: 'Teacher', value: slot.teacherDisplayName ?? `Teacher #${slot.teacherProfileId}` },
          { label: 'Starts', value: formatLocalDateTime(slot.startsAt) },
          { label: 'Ends', value: formatLocalDateTime(slot.endsAt) },
          { label: 'Timezone', value: slot.timezone },
        ]}
      />
      <Button
        isLoading={reserveSlot.isPending}
        loadingLabel="Reserving"
        onClick={() =>
          reserveSlot.mutate(slot.id, {
            onSuccess: (booking) =>
              onMessage(
                `Reserved booking ${booking.id}. Complete payment before the hold expires.`,
              ),
          })
        }
        size="sm"
      >
        Reserve
      </Button>
      {reserveSlot.isError && (
        <Alert title="Reservation failed" variant="danger">
          {normalizeApiError(reserveSlot.error).message}
        </Alert>
      )}
    </RepeatedItemCard>
  )
}

function StudentBookingCard({
  booking,
  onMessage,
}: {
  booking: LiveSpeakingBookingRDO
  onMessage: (message: string) => void
}) {
  const createPayment = useCreateLiveSpeakingBookingPayment()
  const cancelBooking = useCancelLiveSpeakingBooking()
  const canPay = booking.status === 'reserved' && !booking.paymentId
  const canCancel = booking.status === 'reserved' || booking.status === 'booked'

  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone={liveSpeakingStatusTone(booking.status)}>
          {humanizeToken(booking.status)}
        </Badge>
        <Badge tone={booking.meetUrl ? 'success' : 'warning'}>
          {booking.meetUrl ? 'Meet ready' : 'Meet pending'}
        </Badge>
      </div>
      <DescriptionList
        items={[
          { label: 'Teacher', value: booking.teacherDisplayName ?? booking.teacherProfileId },
          { label: 'Starts', value: formatLocalDateTime(booking.startsAt) },
          { label: 'Price', value: formatCurrency(booking.priceAmount, booking.currency, 'Free') },
          {
            label: 'Hold expires',
            value: formatLocalDateTime(booking.holdExpiresAt, 'No active hold'),
          },
          {
            label: 'Payment',
            value: booking.paymentStatus
              ? humanizeToken(String(booking.paymentStatus))
              : booking.paymentId ?? 'Not started',
          },
        ]}
      />
      {booking.meetUrl && (
        <a className="feature-link" href={booking.meetUrl} rel="noreferrer" target="_blank">
          Open Meet link
        </a>
      )}
      <div className="feature-card__actions">
        <Button
          disabled={!canPay}
          isLoading={createPayment.isPending}
          loadingLabel="Starting payment"
          onClick={() =>
            createPayment.mutate(booking.id, {
              onSuccess: (payment) => {
                if (isExternalCheckoutUrl(payment.providerCheckoutUrl)) {
                  window.location.assign(payment.providerCheckoutUrl as string)
                  return
                }

                onMessage(
                  payment.providerCheckoutUrl
                    ? `Payment ${payment.id} created. Checkout: ${payment.providerCheckoutUrl}`
                    : `Payment ${payment.id} created with status ${humanizeToken(payment.status)}.`,
                )
              },
            })
          }
          size="sm"
        >
          Pay
        </Button>
        <Button
          disabled={!canCancel}
          isLoading={cancelBooking.isPending}
          loadingLabel="Cancelling"
          onClick={() =>
            cancelBooking.mutate(booking.id, {
              onSuccess: () => onMessage(`Booking ${booking.id} cancelled.`),
            })
          }
          size="sm"
          variant="secondary"
        >
          Cancel
        </Button>
      </div>
      {(createPayment.isError || cancelBooking.isError) && (
        <Alert title="Booking action failed" variant="danger">
          {normalizeApiError(createPayment.error ?? cancelBooking.error).message}
        </Alert>
      )}
    </RepeatedItemCard>
  )
}

function LiveTeacherSpeakingPanel() {
  const slotsQuery = usePublicScheduleSlots({ page: 0, size: 5, sort: 'startsAt,asc' })
  const bookingsQuery = useStudentScheduleBookings({ page: 0, size: 5, sort: 'startsAt,asc' })
  const [message, setMessage] = useState<string | null>(null)
  const slots = slotsQuery.data?.items ?? []
  const bookings = bookingsQuery.data?.items ?? []

  return (
    <Panel title="Live teacher speaking" description="Reserve a scheduled IELTS speaking session.">
      <div className="feature-panel-body">
        {message && (
          <Alert title="Live speaking updated" variant="success">
            {message}
          </Alert>
        )}

        {slotsQuery.isLoading ? (
          <Loader label="Loading teacher schedules" />
        ) : slotsQuery.isError ? (
          <Alert title="Teacher schedules could not be loaded" variant="danger">
            {normalizeApiError(slotsQuery.error).message}
          </Alert>
        ) : slots.length ? (
          <div className="feature-list">
            {slots.map((slot) => (
              <StudentSlotCard key={slot.id} onMessage={setMessage} slot={slot} />
            ))}
          </div>
        ) : (
          <EmptyState
            title="No announced slots"
            message="Teacher availability will appear here when slots are announced."
          />
        )}

        <div className="feature-stack">
          <h3>Your live sessions</h3>
          {bookingsQuery.isLoading ? (
            <Loader label="Loading bookings" />
          ) : bookingsQuery.isError ? (
            <Alert title="Bookings could not be loaded" variant="danger">
              {normalizeApiError(bookingsQuery.error).message}
            </Alert>
          ) : bookings.length ? (
            <div className="feature-list">
              {bookings.map((booking) => (
                <StudentBookingCard booking={booking} key={booking.id} onMessage={setMessage} />
              ))}
            </div>
          ) : (
            <EmptyState
              title="No live sessions"
              message="Reserved and booked live speaking sessions will appear here."
            />
          )}
        </div>
      </div>
    </Panel>
  )
}

export function SpeakingPracticePage() {
  const [draft, setDraft] = useState<SpeakingDraft>(() => loadDraft())
  const [localAudioFile, setLocalAudioFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [formErrors, setFormErrors] = useState<Partial<Record<SpeakingField, string>>>({})
  const [formMessage, setFormMessage] = useState<string | null>(null)
  const [recordingState, setRecordingState] = useState<RecorderState>('idle')
  const [recorderMessage, setRecorderMessage] = useState<string | null>(
    hasRecordingSupport() ? null : 'Recording is not supported in this browser. Upload an audio file instead.',
  )
  const [feedbackRequested, setFeedbackRequested] = useState(false)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const recordingStartedAtRef = useRef<number | null>(null)
  const feedbackStartedForRef = useRef<BackendId | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const uploadAudio = useUploadSpeakingAudio()
  const createSubmission = useCreateSpeakingSubmission()
  const submissionQuery = useSpeakingSubmission(draft.submissionId, Boolean(draft.submissionId))
  const submission = submissionQuery.data
  const submittedDraftIsVisible = draftMatchesSubmission(draft, submission)
  const draftChangedSinceSubmission = Boolean(draft.submissionId && submission && !submittedDraftIsVisible)
  const hasPersistedSubmission = Boolean(draft.submissionId && !draftChangedSinceSubmission)
  const currentSubmissionId = hasPersistedSubmission ? draft.submissionId : null
  const startTranscription = useStartSpeakingTranscription(currentSubmissionId)
  const shouldPollFeedback = feedbackRequested && Boolean(currentSubmissionId)
  const feedbackQuery = useSpeakingFeedback(currentSubmissionId, shouldPollFeedback, shouldPollFeedback)
  const startFeedback = useStartSpeakingFeedback(currentSubmissionId)
  const retryFeedback = useRetrySpeakingFeedback(currentSubmissionId)
  const aiUsageQuery = useAiUsage(featureFlags.ai)
  const guidance = partGuidance[draft.partType]
  const uploadError = uploadAudio.isError ? normalizeApiError(uploadAudio.error) : null
  const createError = createSubmission.isError ? normalizeApiError(createSubmission.error) : null
  const transcriptionError = startTranscription.isError ? normalizeApiError(startTranscription.error) : null
  const feedbackStartError = startFeedback.isError ? normalizeApiError(startFeedback.error) : null
  const retryError = retryFeedback.isError ? normalizeApiError(retryFeedback.error) : null
  const hasDraftWork = Boolean(draft.prompt.trim() || draft.audioFileId || localAudioFile)
  const isBusy =
    uploadAudio.isPending ||
    createSubmission.isPending ||
    startTranscription.isPending ||
    startFeedback.isPending ||
    retryFeedback.isPending
  const primaryActionLabel = hasPersistedSubmission
    ? submission?.transcriptionStatus === 'failed'
      ? 'Retry transcription'
      : feedbackQuery.data?.status === 'failed'
        ? 'Retry feedback'
        : 'Refresh status'
    : draft.audioFileId
      ? 'Submit speaking answer'
      : 'Upload and submit'

  useEffect(() => {
    saveDraft(draft)
  }, [draft])

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  useEffect(() => {
    return () => {
      streamRef.current?.getTracks().forEach((track) => track.stop())
    }
  }, [])

  useEffect(() => {
    if (!hasDraftWork || submittedDraftIsVisible) {
      return undefined
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasDraftWork, submittedDraftIsVisible])

  useEffect(() => {
    if (!submission || !currentSubmissionId) {
      return
    }

    if (
      submission.transcriptionStatus === 'completed' &&
      submission.transcript?.trim() &&
      feedbackStartedForRef.current !== currentSubmissionId &&
      !feedbackQuery.data &&
      !startFeedback.isPending
    ) {
      feedbackStartedForRef.current = currentSubmissionId
      setFeedbackRequested(true)
      startFeedback.mutate(undefined, {
        onError: () => {
          feedbackStartedForRef.current = null
        },
      })
    }
  }, [currentSubmissionId, feedbackQuery.data, startFeedback, submission])

  const resetSubmissionState = () => {
    setFeedbackRequested(false)
    feedbackStartedForRef.current = null
  }

  const updateDraft = <TKey extends keyof SpeakingDraft>(key: TKey, value: SpeakingDraft[TKey]) => {
    setDraft((current) => {
      const changed = current[key] !== value
      return {
        ...current,
        submissionId: changed && key !== 'submissionId' ? null : current.submissionId,
        [key]: value,
      }
    })
    setFormErrors((current) => ({ ...current, [key]: undefined }))
    setFormMessage(null)
    if (key !== 'submissionId') {
      resetSubmissionState()
    }
  }

  const acceptAudioFile = (file: File, durationSeconds?: number | null) => {
    const normalizedFile = normalizeAudioFile(file)
    const validationError = validateAudioFile(normalizedFile)

    if (validationError) {
      setFormErrors((current) => ({ ...current, audioFileId: validationError }))
      setFormMessage('Choose a supported audio file before submitting.')
      return
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl)
    }

    setLocalAudioFile(normalizedFile)
    setPreviewUrl(URL.createObjectURL(normalizedFile))
    setDraft((current) => ({
      ...current,
      audioContentType: inferredContentType(normalizedFile),
      audioFileId: null,
      audioFileName: normalizedFile.name,
      audioSizeBytes: normalizedFile.size,
      durationSeconds: durationSeconds ?? current.durationSeconds ?? null,
      submissionId: null,
    }))
    setFormErrors((current) => ({ ...current, audioFileId: undefined }))
    setFormMessage(null)
    resetSubmissionState()
  }

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const [file] = Array.from(event.target.files ?? [])
    if (file) {
      acceptAudioFile(file, null)
    }
    event.target.value = ''
  }

  const startRecording = async () => {
    setRecorderMessage(null)

    if (!hasRecordingSupport()) {
      setRecorderMessage('Recording is not supported in this browser. Upload an audio file instead.')
      return
    }

    const mimeType = preferredRecordingType()
    if (!mimeType) {
      setRecorderMessage('This browser cannot record in a supported audio format. Upload an audio file instead.')
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const chunks: Blob[] = []
      const recorder = new MediaRecorder(stream, { mimeType })

      streamRef.current = stream
      recorderRef.current = recorder
      recordingStartedAtRef.current = Date.now()
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunks.push(event.data)
        }
      }
      recorder.onstop = () => {
        const durationSeconds = Math.max(
          1,
          Math.round((Date.now() - (recordingStartedAtRef.current ?? Date.now())) / 1000),
        )
        const blob = new Blob(chunks, { type: mimeType })
        const file = new File(
          [blob],
          `speaking-answer-${Date.now()}.${recordingExtension(mimeType)}`,
          { type: mimeType },
        )
        stream.getTracks().forEach((track) => track.stop())
        streamRef.current = null
        recorderRef.current = null
        recordingStartedAtRef.current = null
        setRecordingState('idle')
        acceptAudioFile(file, durationSeconds)
      }
      recorder.start()
      setRecordingState('recording')
    } catch (error) {
      streamRef.current?.getTracks().forEach((track) => track.stop())
      streamRef.current = null
      recorderRef.current = null
      setRecordingState('idle')
      setRecorderMessage(
        error instanceof DOMException && error.name === 'NotAllowedError'
          ? 'Microphone permission was denied. Upload an audio file instead.'
          : 'Microphone recording could not start. Upload an audio file instead.',
      )
    }
  }

  const stopRecording = () => {
    if (!recorderRef.current || recorderRef.current.state === 'inactive') {
      return
    }

    setRecordingState('stopping')
    recorderRef.current.stop()
  }

  const validate = () => {
    const nextErrors: Partial<Record<SpeakingField, string>> = {}

    if (!draft.prompt.trim()) {
      nextErrors.prompt = 'Enter the speaking prompt before submitting.'
    }

    if (!draft.audioFileId && !localAudioFile) {
      nextErrors.audioFileId = 'Record or upload audio before submitting.'
    }

    setFormErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const ensureUploadedAudio = async () => {
    if (draft.audioFileId) {
      return draft.audioFileId
    }

    if (!localAudioFile) {
      throw new Error('Record or upload audio before submitting.')
    }

    const uploaded = await uploadAudio.mutateAsync(localAudioFile)
    setDraft((current) => ({
      ...current,
      audioContentType: uploaded.contentType,
      audioFileId: uploaded.id,
      audioFileName: current.audioFileName ?? uploaded.filename,
      audioSizeBytes: uploaded.sizeBytes,
    }))
    return uploaded.id
  }

  const handleSubmit = async () => {
    if (!validate()) {
      setFormMessage('Fix the highlighted fields before submitting.')
      return
    }

    setFormMessage(null)

    if (hasPersistedSubmission) {
      try {
        if (submission?.transcriptionStatus === 'failed') {
          await startTranscription.mutateAsync(undefined)
          return
        }

        if (feedbackQuery.data?.status === 'failed') {
          setFeedbackRequested(true)
          await retryFeedback.mutateAsync()
          return
        }

        setFeedbackRequested(true)
        await Promise.all([submissionQuery.refetch(), feedbackQuery.refetch()])
      } catch (error) {
        setFormMessage(error instanceof Error ? error.message : 'Speaking status could not be refreshed.')
      }
      return
    }

    try {
      const audioFileId = await ensureUploadedAudio()
      const nextSubmission = await createSubmission.mutateAsync({
        audioFileId,
        durationSeconds: draft.durationSeconds ?? 1,
        partType: draft.partType,
        prompt: draft.prompt.trim(),
        testAttemptId: null,
      })

      setDraft((current) => ({ ...current, submissionId: nextSubmission.id }))
      const nextTranscription = await startTranscription.mutateAsync(nextSubmission.id)

      if (nextTranscription.transcriptionStatus === 'completed' && nextTranscription.transcript?.trim()) {
        setFeedbackRequested(true)
        await startFeedback.mutateAsync(nextSubmission.id)
      }
    } catch (error) {
      const nextFormState = toFormErrorState(error, fieldNames, 'Speaking submission could not be created.')
      setFormErrors(nextFormState.fieldErrors)
      setFormMessage(nextFormState.formMessage)
    }
  }

  const handleRetryFeedback = async () => {
    setFeedbackRequested(true)
    await retryFeedback.mutateAsync()
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Practice"
        title="Speaking"
        actions={
          <>
            <Link className="feature-link" to={ROUTES.dashboard}>
              Dashboard
            </Link>
            <Link className="feature-link" to={ROUTES.writing}>
              Writing
            </Link>
          </>
        }
      />

      <div className="writing-layout">
        <Panel title="Speaking answer" description="Record or upload one IELTS speaking response.">
          <div className="feature-panel-body">
            {formMessage && (
              <Alert title="Submission blocked" variant="danger">
                {formMessage}
              </Alert>
            )}
            {uploadError && (
              <Alert title="Upload failed" variant="danger">
                {uploadError.message}
              </Alert>
            )}
            {createError?.code === 'AI_USAGE_LIMIT_EXCEEDED' && (
              <Alert title="AI usage limit reached" variant="warning">
                {createError.message}
              </Alert>
            )}
            {transcriptionError && (
              <Alert title="Transcription could not start" variant="danger">
                {transcriptionError.message}
              </Alert>
            )}
            {feedbackStartError && (
              <Alert title="Feedback could not start" variant="warning">
                {feedbackStartError.code === 'TRANSCRIPT_REQUIRED'
                  ? 'Feedback can start after a completed transcript is available.'
                  : feedbackStartError.message}
              </Alert>
            )}

            <SelectField
              label="Speaking part"
              onChange={(event) => updateDraft('partType', event.target.value as SpeakingPartType)}
              value={draft.partType}
            >
              <option value="speaking_part_1">Part 1</option>
              <option value="speaking_part_2">Part 2</option>
              <option value="speaking_part_3">Part 3</option>
            </SelectField>
            <RepeatedItemCard>
              <div className="feature-card__badges">
                <Badge tone="info">{guidance.label}</Badge>
                <Badge tone="neutral">{guidance.time}</Badge>
              </div>
              <p className="feature-card__summary">{guidance.prompt}</p>
            </RepeatedItemCard>
            <TextareaField
              error={formErrors.prompt}
              helpText="Paste or type the exact question or cue-card prompt."
              label="Prompt"
              onChange={(event) => updateDraft('prompt', event.target.value)}
              required
              rows={5}
              value={draft.prompt}
            />

            <div className="speaking-recorder" aria-live="polite">
              <div className="feature-card__badges">
                <Badge tone={recordingState === 'recording' ? 'danger' : draft.audioFileId ? 'success' : 'info'}>
                  {recordingState === 'recording'
                    ? 'Recording'
                    : draft.audioFileId
                      ? `Audio ${draft.audioFileId}`
                      : localAudioFile
                        ? 'Ready to upload'
                        : 'No audio'}
                </Badge>
                {draft.durationSeconds ? <Badge tone="neutral">{formatSeconds(draft.durationSeconds)}</Badge> : null}
                {draft.audioContentType ? <Badge tone="neutral">{draft.audioContentType}</Badge> : null}
              </div>
              {recorderMessage && (
                <Alert title="Recording fallback" variant="warning">
                  {recorderMessage}
                </Alert>
              )}
              {formErrors.audioFileId && (
                <Alert title="Audio required" variant="danger">
                  {formErrors.audioFileId}
                </Alert>
              )}
              {previewUrl && <audio className="speaking-recorder__audio" controls src={previewUrl} />}
              <DescriptionList
                items={[
                  { label: 'File', value: draft.audioFileName ?? 'Not selected' },
                  { label: 'Size', value: formatFileSize(draft.audioSizeBytes) },
                  { label: 'Limit', value: '25 MiB' },
                ]}
              />
              <div className="feature-card__actions">
                {recordingState === 'recording' || recordingState === 'stopping' ? (
                  <Button
                    isLoading={recordingState === 'stopping'}
                    loadingLabel="Stopping"
                    onClick={stopRecording}
                    variant="danger"
                  >
                    Stop recording
                  </Button>
                ) : (
                  <Button disabled={isBusy} onClick={() => void startRecording()} variant="secondary">
                    Record audio
                  </Button>
                )}
                <Button
                  disabled={isBusy || recordingState !== 'idle'}
                  onClick={() => fileInputRef.current?.click()}
                  variant="secondary"
                >
                  Upload audio
                </Button>
                <input
                  accept="audio/aac,audio/m4a,audio/mp3,audio/mp4,audio/mpeg,audio/ogg,audio/wav,audio/webm,audio/x-m4a,audio/x-wav,.aac,.m4a,.mp3,.mp4,.oga,.ogg,.wav,.webm"
                  className="speaking-recorder__file"
                  id="speaking-audio-upload"
                  onChange={handleFileChange}
                  ref={fileInputRef}
                  type="file"
                />
              </div>
            </div>

            <div className="feature-card__actions">
              <Button
                disabled={isBusy || recordingState !== 'idle'}
                isLoading={isBusy}
                loadingLabel="Working"
                onClick={() => void handleSubmit()}
              >
                {primaryActionLabel}
              </Button>
              <Button
                disabled={!draft.submissionId || submissionQuery.isFetching}
                isLoading={submissionQuery.isFetching || feedbackQuery.isFetching}
                loadingLabel="Refreshing"
                onClick={() => {
                  setFeedbackRequested(true)
                  void Promise.all([submissionQuery.refetch(), feedbackQuery.refetch()])
                }}
                variant="secondary"
              >
                Refresh status
              </Button>
            </div>
          </div>
        </Panel>

        <aside className="writing-side">
          {featureFlags.ai && (
            <Panel title="AI usage" description="Current request allowance.">
              <div className="feature-panel-body">
                {aiUsageQuery.isLoading ? (
                  <Loader label="Loading usage" />
                ) : aiUsageQuery.data ? (
                  <RepeatedItemCard>
                    <Badge tone={aiUsageQuery.data.hardLimitReached ? 'danger' : 'success'}>
                      {aiUsageQuery.data.hardLimitReached ? 'Limit reached' : 'Available'}
                    </Badge>
                    <DescriptionList
                      items={[
                        { label: 'Free left', value: aiUsageQuery.data.freeRequestsRemaining },
                        { label: 'Hard left', value: aiUsageQuery.data.hardRequestsRemaining ?? 'No hard cap' },
                        { label: 'Used', value: aiUsageQuery.data.usedRequests },
                        {
                          label: 'Period',
                          value: `${formatDate(aiUsageQuery.data.periodStart)} - ${formatDate(aiUsageQuery.data.periodEnd)}`,
                        },
                      ]}
                    />
                  </RepeatedItemCard>
                ) : (
                  <EmptyState title="Usage unavailable" message="AI usage could not be loaded." />
                )}
              </div>
            </Panel>
          )}

          <Panel title="Current submission" description="Reload-safe from this browser's last submission id.">
            <div className="feature-panel-body">
              {submissionQuery.isLoading ? (
                <Loader label="Loading submission" />
              ) : submission ? (
                <RepeatedItemCard>
                  <div className="feature-card__badges">
                    <Badge tone="info">{humanizeToken(submission.partType)}</Badge>
                    <Badge tone={submission.transcriptionStatus === 'failed' ? 'danger' : 'success'}>
                      {humanizeToken(submission.transcriptionStatus)}
                    </Badge>
                  </div>
                  <DescriptionList
                    items={[
                      { label: 'Submitted', value: formatDate(submission.createdAt) },
                      { label: 'Audio', value: submission.audioFileId },
                      { label: 'Duration', value: formatSeconds(submission.durationSeconds) },
                      { label: 'Provider', value: submission.provider ?? 'Not reported' },
                    ]}
                  />
                </RepeatedItemCard>
              ) : (
                <EmptyState title="No submission yet" message="Submit audio to track transcription here." />
              )}
            </div>
          </Panel>

          <LiveTeacherSpeakingPanel />
        </aside>
      </div>

      <Panel title="Transcript" description="Transcription status and recovered transcript.">
        <div className="feature-panel-body">
          {submission?.transcriptionStatus === 'queued' || submission?.transcriptionStatus === 'processing' ? (
            <div className="feature-panel-state">
              <Loader
                label={
                  submission.transcriptionStatus === 'queued'
                    ? 'Transcription queued'
                    : 'Transcription processing'
                }
              />
              <p className="feature-card__summary">The original audio stays stored while transcription runs.</p>
            </div>
          ) : submission?.transcriptionStatus === 'failed' ? (
            <div className="feature-panel-state">
              <Alert title="Transcription failed" variant="danger">
                {submission.errorMessage ??
                  (submission.failureCode ? humanizeToken(submission.failureCode) : 'Transcription could not complete.')}
              </Alert>
              <div className="feature-card__actions">
                <Button
                  isLoading={startTranscription.isPending}
                  loadingLabel="Retrying"
                  onClick={() => void startTranscription.mutateAsync(undefined)}
                >
                  Retry transcription
                </Button>
                <Button
                  isLoading={submissionQuery.isFetching}
                  loadingLabel="Refreshing"
                  onClick={() => void submissionQuery.refetch()}
                  variant="secondary"
                >
                  Refresh
                </Button>
              </div>
            </div>
          ) : submission?.transcript ? (
            <pre className="writing-pre">{submission.transcript}</pre>
          ) : (
            <EmptyState title="No transcript yet" message="Submit audio to start transcription." />
          )}
        </div>
      </Panel>

      <Panel title="AI speaking feedback" description="Queued, processing, failed, and completed feedback states.">
        <div className="feature-panel-body">
          {retryError && (
            <Alert title="Retry failed" variant="danger">
              {retryError.message}
            </Alert>
          )}
          {feedbackQuery.isLoading && currentSubmissionId ? (
            <Loader label="Loading feedback" />
          ) : feedbackQuery.isError ? (
            <Alert title="Feedback could not be loaded" variant="warning">
              {normalizeApiError(feedbackQuery.error).code === 'AI_FEEDBACK_NOT_FOUND'
                ? 'Feedback will start after the transcript is ready.'
                : normalizeApiError(feedbackQuery.error).message}
            </Alert>
          ) : (
            <FeedbackPanel
              feedback={feedbackQuery.data}
              isFetching={feedbackQuery.isFetching}
              onRefresh={() => {
                setFeedbackRequested(true)
                void feedbackQuery.refetch()
              }}
              onRetry={() => void handleRetryFeedback()}
              retrying={retryFeedback.isPending}
            />
          )}
        </div>
      </Panel>
    </PageContainer>
  )
}
