import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ROUTES, fillRoute } from '../../constants/routes'
import { useAttemptResult, useSaveAnswer, useStartAttempt, useSubmitAttempt } from '../../hooks/useAttemptLifecycle'
import { useFileDownloadUrl } from '../../hooks/useFiles'
import type {
  BackendId,
  RichTextContentRDO,
  StudentAnswerUDO,
  StudentListeningSegmentRDO,
  StudentQuestionRDO,
  StudentTestAttemptRDO,
  TestDetailRDO,
  TestDetailSectionRDO,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatMinutes, formatSeconds, humanizeToken } from '../../utils/formatters'
import { Alert, Badge, Button, Dialog, EmptyState, PlayerLayout, RichContent, SplitPane } from '../ui'

type DraftState = {
  activeSectionId?: BackendId
  answers: Record<string, string>
  attempt: StudentTestAttemptRDO | null
  currentTimes: Record<string, number>
  persistedAnswers: Record<string, string>
  testId: BackendId
}

type AudioState = {
  currentTime: number
  duration: number | null
  error: string | null
  isPlaying: boolean
}

const storageKey = (testId: BackendId) => `mock-exams.listening-attempt.${testId}`
const emptyDraft = (testId: BackendId): DraftState => ({
  answers: {},
  attempt: null,
  currentTimes: {},
  persistedAnswers: {},
  testId,
})

const loadDraft = (testId: BackendId): DraftState => {
  if (typeof window === 'undefined') {
    return emptyDraft(testId)
  }

  const rawValue = window.localStorage.getItem(storageKey(testId))
  if (!rawValue) {
    return emptyDraft(testId)
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<DraftState>
    return parsed.testId === testId
      ? {
          activeSectionId: parsed.activeSectionId,
          answers: parsed.answers ?? {},
          attempt: parsed.attempt ?? null,
          currentTimes: parsed.currentTimes ?? {},
          persistedAnswers: parsed.persistedAnswers ?? {},
          testId,
        }
      : emptyDraft(testId)
  } catch {
    window.localStorage.removeItem(storageKey(testId))
    return emptyDraft(testId)
  }
}

const saveDraft = (draft: DraftState) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(storageKey(draft.testId), JSON.stringify(draft))
  }
}

const clearDraft = (testId: BackendId) => {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(storageKey(testId))
  }
}

const contentHtml = (content?: RichTextContentRDO | null) => content?.renderedHtml ?? undefined
const contentText = (content?: RichTextContentRDO | null) => content?.contentText ?? undefined
const attemptLifecycleErrorCodes = new Set([
  'ATTEMPT_ALREADY_SUBMITTED',
  'ATTEMPT_EXPIRED',
  'ATTEMPT_STATUS_CONFLICT',
])

const orderedSections = (test: TestDetailRDO) =>
  [...test.sections].sort((a, b) => a.sectionOrder - b.sectionOrder)

const sectionQuestions = (section: TestDetailSectionRDO) =>
  [...(section.questions ?? [])].sort((a, b) => a.questionOrder - b.questionOrder)

const orderedSegments = (section: TestDetailSectionRDO) =>
  [...(section.listeningSegments ?? [])].sort((a, b) => a.segmentOrder - b.segmentOrder)

const allQuestions = (sections: TestDetailSectionRDO[]) => sections.flatMap(sectionQuestions)
const questionKey = (questionId: BackendId) => String(questionId)
const sectionKey = (sectionId: BackendId) => String(sectionId)

const getDraftAnswer = (draft: DraftState, questionId: BackendId) =>
  draft.answers[questionKey(questionId)]?.trim() ?? ''

const getPersistedAnswer = (draft: DraftState, questionId: BackendId) =>
  draft.persistedAnswers[questionKey(questionId)] ?? ''

const hasPersistedAnswer = (draft: DraftState, questionId: BackendId) =>
  Object.prototype.hasOwnProperty.call(draft.persistedAnswers, questionKey(questionId))

const isAnswerSaved = (draft: DraftState, questionId: BackendId) => {
  const value = getDraftAnswer(draft, questionId)
  return Boolean(value) && value === getPersistedAnswer(draft, questionId)
}

const toAnswerPayload = (question: StudentQuestionRDO, value: string): StudentAnswerUDO | null => {
  if (!value) {
    return null
  }

  const selectedOptionId = question.options?.length ? Number(value) : undefined

  return {
    answerText: selectedOptionId ? null : value,
    audioFileId: null,
    questionId: question.id,
    selectedOptionId: selectedOptionId || null,
  }
}

const segmentQuestionLabel = (questions: StudentQuestionRDO[]) => {
  if (questions.length === 0) {
    return 'No linked questions'
  }

  const orders = questions.map((question) => question.questionOrder).sort((a, b) => a - b)
  return orders.length === 1 ? `Question ${orders[0]}` : `Questions ${orders[0]}-${orders[orders.length - 1]}`
}

const segmentIsCurrent = (segment: StudentListeningSegmentRDO, currentTime: number) => {
  const start = segment.startSeconds ?? null
  const end = segment.endSeconds ?? null

  if (start === null && end === null) {
    return false
  }

  if (start !== null && currentTime < start) {
    return false
  }

  return end === null || currentTime <= end
}

function AnswerInput({
  disabled,
  onChange,
  question,
  value,
}: {
  disabled: boolean
  onChange: (value: string) => void
  question: StudentQuestionRDO
  value: string
}) {
  if (question.options?.length) {
    return (
      <fieldset className="reading-options" disabled={disabled}>
        <legend>Choose one answer</legend>
        {[...question.options]
          .sort((a, b) => a.optionOrder - b.optionOrder)
          .map((option) => (
            <label key={option.id} className="reading-option">
              <input
                checked={value === String(option.id)}
                name={`question-${question.id}`}
                onChange={() => onChange(String(option.id))}
                type="radio"
              />
              <span>{option.optionLabel && <strong>{option.optionLabel}.</strong>}</span>
              {contentHtml(option.optionTextContent) ? (
                <RichContent trustedHtml={contentHtml(option.optionTextContent)} />
              ) : (
                <span>{contentText(option.optionTextContent) ?? option.optionText}</span>
              )}
            </label>
          ))}
      </fieldset>
    )
  }

  return (
    <label className="reading-text-answer">
      <span>Answer</span>
      <input
        className="ui-input"
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Type your answer"
        value={value}
      />
    </label>
  )
}

export function ListeningTestPlayer({ test }: { test: TestDetailRDO }) {
  const navigate = useNavigate()
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const sections = useMemo(() => orderedSections(test), [test])
  const questions = useMemo(() => allQuestions(sections), [sections])
  const [draft, setDraft] = useState<DraftState>(() => loadDraft(test.id))
  const [activeSectionId, setActiveSectionId] = useState(draft.activeSectionId ?? sections[0]?.id)
  const [now, setNow] = useState(() => Date.now())
  const [savingQuestionId, setSavingQuestionId] = useState<BackendId | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [blockedNavigationPath, setBlockedNavigationPath] = useState<string | null>(null)
  const [submitOpen, setSubmitOpen] = useState(false)
  const [exitOpen, setExitOpen] = useState(false)
  const [audioState, setAudioState] = useState<AudioState>({
    currentTime: 0,
    duration: null,
    error: null,
    isPlaying: false,
  })
  const startAttempt = useStartAttempt(test.id)
  const saveAnswer = useSaveAnswer(draft.attempt?.id)
  const submitAttempt = useSubmitAttempt(draft.attempt?.id)
  const resultQuery = useAttemptResult(
    draft.attempt?.id,
    draft.attempt?.status === 'submitted' ||
      draft.attempt?.status === 'expired' ||
      draft.attempt?.status === 'scored',
  )
  const activeSection = sections.find((section) => section.id === activeSectionId) ?? sections[0]
  const activeAudioFileId = activeSection?.audioFileId ?? null
  const currentTimesRef = useRef(draft.currentTimes)
  const audioQuery = useFileDownloadUrl(activeAudioFileId)
  const activeSegments = activeSection ? orderedSegments(activeSection) : []
  const unansweredCount = questions.filter((question) => !getDraftAnswer(draft, question.id)).length
  const expiresAt = draft.attempt?.expiresAt ? Date.parse(draft.attempt.expiresAt) : undefined
  const remainingSeconds =
    expiresAt && Number.isFinite(expiresAt)
      ? Math.max(0, Math.floor((expiresAt - now) / 1000))
      : draft.attempt?.remainingSeconds
  const isExpired = draft.attempt?.status === 'expired' || remainingSeconds === 0
  const isSubmitted =
    draft.attempt?.status === 'submitted' || draft.attempt?.status === 'scored' || draft.attempt?.status === 'expired'
  const canAnswer = Boolean(draft.attempt) && !isSubmitted && !isExpired
  const resultError = resultQuery.isError ? normalizeApiError(resultQuery.error) : null
  const dirtyAnswerQuestions = questions.filter((question) => {
    const value = getDraftAnswer(draft, question.id)
    return Boolean(value) && value !== getPersistedAnswer(draft, question.id)
  })
  const clearedPersistedQuestions = questions.filter((question) => {
    const value = getDraftAnswer(draft, question.id)
    return !value && hasPersistedAnswer(draft, question.id) && getPersistedAnswer(draft, question.id)
  })
  const pendingSaveCount = dirtyAnswerQuestions.length
  const hasUnsavedAnswers = pendingSaveCount > 0 || clearedPersistedQuestions.length > 0
  const isSubmitting = submitAttempt.isPending || saveAnswer.isPending
  const isAttemptFinalizing = Boolean(
    draft.attempt &&
      isSubmitted &&
      !resultQuery.data?.id &&
      (resultQuery.isLoading || resultQuery.isFetching || resultError),
  )

  useEffect(() => {
    saveDraft({ ...draft, activeSectionId })
  }, [activeSectionId, draft])

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [])

  useEffect(() => {
    currentTimesRef.current = draft.currentTimes
  }, [draft.currentTimes])

  useEffect(() => {
    if (!hasUnsavedAnswers) {
      return undefined
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedAnswers])

  useEffect(() => {
    if (!hasUnsavedAnswers || typeof document === 'undefined') {
      return undefined
    }

    const handleDocumentClick = (event: MouseEvent) => {
      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        event.altKey ||
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey
      ) {
        return
      }

      const target = event.target instanceof Element ? event.target : null
      const anchor = target?.closest('a[href]') as HTMLAnchorElement | null
      if (!anchor || anchor.target || anchor.hasAttribute('download')) {
        return
      }

      const href = anchor.getAttribute('href')
      if (!href || href.startsWith('#')) {
        return
      }

      const nextUrl = new URL(anchor.href, window.location.href)
      if (nextUrl.origin !== window.location.origin) {
        return
      }

      const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
      const nextPath = `${nextUrl.pathname}${nextUrl.search}${nextUrl.hash}`
      if (nextPath === currentPath) {
        return
      }

      event.preventDefault()
      event.stopPropagation()
      setBlockedNavigationPath(nextPath)
    }

    document.addEventListener('click', handleDocumentClick, true)
    return () => document.removeEventListener('click', handleDocumentClick, true)
  }, [hasUnsavedAnswers])

  useEffect(() => {
    if (resultQuery.data?.id) {
      clearDraft(test.id)
      navigate(fillRoute(ROUTES.resultDetail, { id: resultQuery.data.id }))
    }
  }, [navigate, resultQuery.data?.id, test.id])

  useEffect(() => {
    const fileUrl = audioQuery.data
    if (!fileUrl) {
      return undefined
    }

    const expiresAtMs = fileUrl.expiresAt
      ? Date.parse(fileUrl.expiresAt)
      : fileUrl.expiresInSeconds
        ? Date.now() + fileUrl.expiresInSeconds * 1000
        : null

    if (!expiresAtMs || !Number.isFinite(expiresAtMs)) {
      return undefined
    }

    const delayMs = Math.max(30_000, expiresAtMs - Date.now() - 60_000)
    const timeout = window.setTimeout(() => void audioQuery.refetch(), delayMs)
    return () => window.clearTimeout(timeout)
  }, [audioQuery, audioQuery.data])

  useEffect(() => {
    setAudioState((current) => ({
      ...current,
      currentTime: activeSection ? currentTimesRef.current[sectionKey(activeSection.id)] ?? 0 : 0,
      duration: null,
      error: null,
      isPlaying: false,
    }))
  }, [activeSection])

  const updateAnswer = (questionId: BackendId, value: string) => {
    setDraft((current) => ({
      ...current,
      answers: {
        ...current.answers,
        [questionId]: value,
      },
    }))
    setSaveError(null)
    setSubmitError(null)
  }

  const updateCurrentTime = (sectionId: BackendId, currentTime: number) => {
    setDraft((current) => {
      const existing = current.currentTimes[sectionKey(sectionId)] ?? 0
      if (Math.abs(existing - currentTime) < 1) {
        return current
      }

      return {
        ...current,
        currentTimes: {
          ...current.currentTimes,
          [sectionKey(sectionId)]: currentTime,
        },
      }
    })
  }

  const updatePersistedAnswer = (questionId: BackendId, value: string) => {
    setDraft((current) => ({
      ...current,
      persistedAnswers: {
        ...current.persistedAnswers,
        [questionKey(questionId)]: value,
      },
    }))
  }

  const markAttemptStatus = (status: StudentTestAttemptRDO['status']) => {
    setDraft((current) =>
      current.attempt
        ? {
            ...current,
            attempt: {
              ...current.attempt,
              remainingSeconds: status === 'expired' ? 0 : current.attempt.remainingSeconds,
              status,
            },
          }
        : current,
    )
  }

  const handleAttemptLifecycleError = (error: unknown) => {
    const apiError = normalizeApiError(error)

    if (!attemptLifecycleErrorCodes.has(apiError.code)) {
      return false
    }

    if (apiError.code === 'ATTEMPT_EXPIRED') {
      saveAnswer.reset()
      submitAttempt.reset()
      markAttemptStatus('expired')
      setSaveError(null)
      setSubmitError(
        'Time expired before every draft answer could be saved. The result will open automatically when the backend finishes scoring persisted answers.',
      )
      setSubmitOpen(false)
      return true
    }

    saveAnswer.reset()
    submitAttempt.reset()
    markAttemptStatus(isExpired ? 'expired' : 'submitted')
    setSubmitOpen(false)
    setSubmitError(`${apiError.message} The result will open automatically when it is ready.`)
    return true
  }

  const handleStart = async () => {
    const attempt = await startAttempt.mutateAsync()
    setDraft((current) => ({ ...current, attempt }))
  }

  const handleSave = async (question: StudentQuestionRDO) => {
    const value = getDraftAnswer(draft, question.id)
    if (!value || !draft.attempt) {
      return
    }

    setSavingQuestionId(question.id)
    setSaveError(null)

    try {
      const payload = toAnswerPayload(question, value)
      if (!payload) {
        return
      }

      await saveAnswer.mutateAsync(payload)
      updatePersistedAnswer(question.id, value)
    } catch (error) {
      if (!handleAttemptLifecycleError(error)) {
        setSaveError(error instanceof Error ? error.message : 'Answer could not be saved.')
      }
    } finally {
      setSavingQuestionId(null)
    }
  }

  const saveDirtyAnswers = async () => {
    if (clearedPersistedQuestions.length > 0) {
      throw new Error(
        'One or more saved answers were cleared locally. Enter a replacement answer and save again before submitting.',
      )
    }

    for (const question of dirtyAnswerQuestions) {
      const value = getDraftAnswer(draft, question.id)
      const payload = toAnswerPayload(question, value)

      if (!payload) {
        continue
      }

      setSavingQuestionId(question.id)
      await saveAnswer.mutateAsync(payload)
      updatePersistedAnswer(question.id, value)
    }
  }

  const handleSubmit = async () => {
    if (!draft.attempt) {
      return
    }

    setSaveError(null)
    setSubmitError(null)

    try {
      await saveDirtyAnswers()
      setSavingQuestionId(null)
      const attempt = await submitAttempt.mutateAsync()
      setDraft((current) => ({ ...current, attempt }))
      setSubmitOpen(false)
    } catch (error) {
      setSavingQuestionId(null)
      if (!handleAttemptLifecycleError(error)) {
        setSubmitError(error instanceof Error ? error.message : 'Save your answers and try submitting again.')
      }
    }
  }

  const handleSectionChange = (sectionId: BackendId) => {
    setActiveSectionId(sectionId)
    setDraft((current) => ({ ...current, activeSectionId: sectionId }))
  }

  const handleAudioLoaded = () => {
    const audio = audioRef.current
    if (!audio || !activeSection) {
      return
    }

    const savedTime = draft.currentTimes[sectionKey(activeSection.id)] ?? 0
    if (savedTime > 0 && savedTime < audio.duration) {
      audio.currentTime = savedTime
    }

    setAudioState((current) => ({
      ...current,
      currentTime: audio.currentTime,
      duration: Number.isFinite(audio.duration) ? audio.duration : null,
      error: null,
    }))
  }

  const handleAudioTimeUpdate = () => {
    const audio = audioRef.current
    if (!audio || !activeSection) {
      return
    }

    setAudioState((current) => ({
      ...current,
      currentTime: audio.currentTime,
      duration: Number.isFinite(audio.duration) ? audio.duration : current.duration,
    }))
    updateCurrentTime(activeSection.id, audio.currentTime)
  }

  const handleAudioError = () => {
    setAudioState((current) => ({
      ...current,
      error: 'Audio could not be loaded. Refresh the signed URL and try again.',
      isPlaying: false,
    }))
    void audioQuery.refetch()
  }

  const refreshAudio = async () => {
    setAudioState((current) => ({ ...current, error: null, isPlaying: false }))
    await audioQuery.refetch()
    audioRef.current?.load()
  }

  const togglePlayback = async () => {
    const audio = audioRef.current
    if (!audio || !audioQuery.data?.url || audioState.error) {
      return
    }

    if (audio.paused) {
      try {
        await audio.play()
        setAudioState((current) => ({ ...current, isPlaying: true, error: null }))
      } catch {
        setAudioState((current) => ({
          ...current,
          error: 'Playback could not start. Refresh the audio link and try again.',
          isPlaying: false,
        }))
      }
      return
    }

    audio.pause()
    setAudioState((current) => ({ ...current, isPlaying: false }))
  }

  const seekToSegment = (segment: StudentListeningSegmentRDO) => {
    const audio = audioRef.current
    const nextTime = segment.startSeconds ?? 0
    if (audio) {
      audio.currentTime = nextTime
    }
    setAudioState((current) => ({ ...current, currentTime: nextTime }))
    if (activeSection) {
      updateCurrentTime(activeSection.id, nextTime)
    }
  }

  const leaveListeningAttempt = (path: string) => {
    setBlockedNavigationPath(null)
    setExitOpen(false)
    navigate(path)
  }

  if (sections.length === 0 || questions.length === 0) {
    return (
      <EmptyState
        title="Listening test is not ready"
        message="This published test has no listening sections or questions yet."
        actions={
          <Link className="feature-link" to={ROUTES.tests}>
            Back to tests
          </Link>
        }
      />
    )
  }

  if (!draft.attempt) {
    return (
      <div className="reading-prestart">
        <div className="feature-card__badges">
          <Badge tone="info">{formatMinutes(test.durationMinutes)}</Badge>
          <Badge tone={test.free ? 'success' : 'warning'}>{test.free ? 'Free' : 'Requires access'}</Badge>
          <Badge tone="info">{questions.length} questions</Badge>
        </div>
        <p>
          Start creates a timed backend attempt. Listening audio is loaded through a signed backend
          URL, and draft answers stay in this browser until each answer is saved.
        </p>
        {startAttempt.isError && (
          <Alert title="Attempt could not start" variant="danger">
            {startAttempt.error instanceof Error
              ? startAttempt.error.message
              : 'Log in as a student or check access to this test.'}
          </Alert>
        )}
        <Button isLoading={startAttempt.isPending} loadingLabel="Starting" onClick={handleStart}>
          Start Listening test
        </Button>
      </div>
    )
  }

  const sidebar = (
    <div className="reading-sidebar">
      <Badge tone={isExpired ? 'danger' : isSubmitted ? 'success' : 'info'}>
        {isExpired ? 'Expired' : isSubmitted ? 'Submitted' : 'In progress'}
      </Badge>
      <p className="reading-timer">{formatSeconds(remainingSeconds, 'Untimed')}</p>
      <p>{unansweredCount} unanswered</p>

      <div className="listening-audio">
        <audio
          key={`${activeSection?.id ?? 'no-section'}-${audioQuery.data?.url ?? 'no-url'}`}
          onCanPlay={() => setAudioState((current) => ({ ...current, error: null }))}
          onEnded={() => setAudioState((current) => ({ ...current, isPlaying: false }))}
          onError={handleAudioError}
          onLoadedMetadata={handleAudioLoaded}
          onPause={() => setAudioState((current) => ({ ...current, isPlaying: false }))}
          onPlay={() => setAudioState((current) => ({ ...current, isPlaying: true }))}
          onTimeUpdate={handleAudioTimeUpdate}
          preload="metadata"
          ref={audioRef}
          src={audioQuery.data?.url}
        />
        <div className="feature-card__badges">
          <Badge tone={activeAudioFileId ? 'info' : 'warning'}>
            {activeAudioFileId ? `Audio ${activeAudioFileId}` : 'No audio'}
          </Badge>
          {audioQuery.isFetching && <Badge tone="info">Refreshing</Badge>}
        </div>
        <p className="listening-audio__time">
          {formatSeconds(Math.floor(audioState.currentTime), '0:00')} /{' '}
          {formatSeconds(audioState.duration ? Math.floor(audioState.duration) : null, '0:00')}
        </p>
        {audioQuery.isError && (
          <Alert title="Audio URL failed" variant="warning">
            {audioQuery.error instanceof Error
              ? audioQuery.error.message
              : 'The signed audio URL could not be loaded.'}
          </Alert>
        )}
        {audioState.error && (
          <Alert title="Audio playback failed" variant="warning">
            {audioState.error}
          </Alert>
        )}
        <div className="feature-card__actions">
          <Button
            disabled={!audioQuery.data?.url || Boolean(audioState.error)}
            isLoading={audioQuery.isLoading}
            loadingLabel="Loading"
            onClick={togglePlayback}
            size="sm"
            variant="secondary"
          >
            {audioState.isPlaying ? 'Pause' : 'Play'}
          </Button>
          <Button
            disabled={!activeAudioFileId || audioQuery.isFetching}
            isLoading={audioQuery.isFetching}
            loadingLabel="Refreshing"
            onClick={() => void refreshAudio()}
            size="sm"
            variant="secondary"
          >
            Refresh audio
          </Button>
        </div>
      </div>

      <div className="reading-section-nav">
        {sections.map((section) => (
          <button
            className={section.id === activeSection.id ? 'active' : undefined}
            key={section.id}
            onClick={() => handleSectionChange(section.id)}
            type="button"
          >
            {section.title}
          </button>
        ))}
      </div>
      <div className="reading-answer-sheet" aria-label="Answer sheet">
        {questions.map((question, index) => {
          const answered = Boolean(draft.answers[String(question.id)])
          const saved = isAnswerSaved(draft, question.id)

          return (
            <button
              className={answered ? 'answered' : undefined}
              key={question.id}
              onClick={() => {
                const section = sections.find((item) =>
                  sectionQuestions(item).some((sectionQuestion) => sectionQuestion.id === question.id),
                )
                if (section) {
                  handleSectionChange(section.id)
                }
                document.getElementById(`question-${question.id}`)?.scrollIntoView({ behavior: 'smooth' })
              }}
              title={saved ? 'Saved' : answered ? 'Draft answer' : 'Unanswered'}
              type="button"
            >
              {index + 1}
            </button>
          )
        })}
      </div>
    </div>
  )

  const source = (
    <div className="reading-passage">
      <h2>{activeSection.title}</h2>
      {activeSection.instructionsContent?.renderedHtml ? (
        <RichContent trustedHtml={activeSection.instructionsContent.renderedHtml} />
      ) : (
        activeSection.instructions && <RichContent blocks={[{ text: activeSection.instructions, type: 'paragraph' }]} />
      )}
      <div className="listening-segments">
        {activeSegments.length > 0 ? (
          activeSegments.map((segment) => {
            const linkedQuestions = sectionQuestions(activeSection).filter(
              (question) => question.listeningSegmentId === segment.id,
            )
            const current = segmentIsCurrent(segment, audioState.currentTime)

            return (
              <button
                className={current ? 'active' : undefined}
                key={segment.id}
                onClick={() => seekToSegment(segment)}
                type="button"
              >
                <span>{segment.title}</span>
                <strong>{segmentQuestionLabel(linkedQuestions)}</strong>
                <small>
                  {formatSeconds(segment.startSeconds ?? null, '0:00')} -{' '}
                  {formatSeconds(segment.endSeconds ?? null, 'open')}
                </small>
              </button>
            )
          })
        ) : (
          <Alert title="No segment metadata" variant="info">
            You can still answer every question in this section. Segment markers will appear here
            when the test author links them to the audio.
          </Alert>
        )}
      </div>
    </div>
  )

  const workspace = (
    <div className="reading-questions">
      {saveError && (
        <Alert title="Save failed" variant="danger">
          {saveError}
        </Alert>
      )}
      {isExpired && (
        <Alert title="Time expired" variant="warning">
          This attempt can no longer accept answer changes. The result will open automatically when it is ready.
        </Alert>
      )}
      {isAttemptFinalizing && (
        <Alert title="Result pending" variant={resultError ? 'warning' : 'info'}>
          {resultError
            ? resultError.message
            : 'The backend is finalizing this attempt. You can check again while we wait.'}
          <div>
            <Button
              disabled={resultQuery.isFetching}
              isLoading={resultQuery.isFetching}
              loadingLabel="Checking"
              onClick={() => void resultQuery.refetch()}
              size="sm"
              variant="secondary"
            >
              Check result
            </Button>
          </div>
        </Alert>
      )}
      {sectionQuestions(activeSection).map((question) => {
        const value = draft.answers[String(question.id)] ?? ''
        const saved = isAnswerSaved(draft, question.id)
        const segment = activeSegments.find((item) => item.id === question.listeningSegmentId)

        return (
          <article className="reading-question" id={`question-${question.id}`} key={question.id}>
            <div className="feature-card__badges">
              <Badge tone={value ? 'success' : 'warning'}>Question {question.questionOrder}</Badge>
              <Badge tone="info">{humanizeToken(question.questionType)}</Badge>
              {segment && <Badge tone="info">{segment.title}</Badge>}
              {saved && <Badge tone="success">Saved</Badge>}
            </div>
            {question.instructionsContent?.renderedHtml ? (
              <RichContent trustedHtml={question.instructionsContent.renderedHtml} />
            ) : (
              question.instructions && <p className="feature-card__summary">{question.instructions}</p>
            )}
            {question.questionTextContent?.renderedHtml ? (
              <RichContent trustedHtml={question.questionTextContent.renderedHtml} />
            ) : (
              <p>{question.questionText}</p>
            )}
            <AnswerInput
              disabled={!canAnswer}
              onChange={(nextValue) => updateAnswer(question.id, nextValue)}
              question={question}
              value={value}
            />
            <Button
              disabled={!value || !canAnswer || saved}
              isLoading={savingQuestionId === question.id}
              loadingLabel="Saving"
              onClick={() => handleSave(question)}
              size="sm"
              variant="secondary"
            >
              Save answer
            </Button>
          </article>
        )
      })}
    </div>
  )

  return (
    <>
      <PlayerLayout
        sidebar={sidebar}
        footer={
          <div className="reading-footer">
            {hasUnsavedAnswers ? (
              <Button onClick={() => setExitOpen(true)} variant="secondary">
                Exit test
              </Button>
            ) : (
              <Link className="feature-link" to={ROUTES.tests}>
                Exit test
              </Link>
            )}
            <Button
              disabled={!draft.attempt || isSubmitted}
              isLoading={isSubmitting || resultQuery.isFetching}
              loadingLabel={pendingSaveCount ? 'Saving answers' : 'Submitting'}
              onClick={() => setSubmitOpen(true)}
              variant="primary"
            >
              Submit attempt
            </Button>
          </div>
        }
      >
        <SplitPane left={source} leftLabel="Listening audio segments" right={workspace} rightLabel="Questions" />
      </PlayerLayout>

      <Dialog
        description={
          unansweredCount
            ? `${unansweredCount} question${unansweredCount === 1 ? '' : 's'} still need an answer.`
            : pendingSaveCount
              ? `${pendingSaveCount} draft answer${pendingSaveCount === 1 ? '' : 's'} will be saved before submission.`
              : 'All draft answers have been saved.'
        }
        isOpen={submitOpen}
        onOpenChange={setSubmitOpen}
        title="Submit Listening attempt?"
        footer={
          <>
            <Button onClick={() => setSubmitOpen(false)} variant="secondary">
              Keep working
            </Button>
            <Button
              isLoading={isSubmitting}
              loadingLabel={pendingSaveCount ? 'Saving answers' : 'Submitting'}
              onClick={handleSubmit}
            >
              Submit
            </Button>
          </>
        }
      >
        <p>
          Submitted attempts cannot be edited. Draft answers are saved before submit, and the backend
          scores the persisted answers.
        </p>
        {submitAttempt.isError && (
          <Alert title="Submit failed" variant="danger">
            {submitAttempt.error instanceof Error
              ? submitAttempt.error.message
              : 'Try saving answers and submitting again.'}
          </Alert>
        )}
        {submitError && (
          <Alert title="Submit blocked" variant="danger">
            {submitError}
          </Alert>
        )}
      </Dialog>

      <Dialog
        description="You have local answer changes that have not been saved to the backend."
        isOpen={exitOpen}
        onOpenChange={setExitOpen}
        title="Leave Listening attempt?"
        footer={
          <>
            <Button onClick={() => setExitOpen(false)} variant="secondary">
              Keep working
            </Button>
            <Button onClick={() => leaveListeningAttempt(ROUTES.tests)}>
              Leave anyway
            </Button>
          </>
        }
      >
        <p>Save each changed answer or submit the attempt before leaving if you want the backend to keep it.</p>
      </Dialog>

      <Dialog
        description="You have local answer changes that have not been saved to the backend."
        isOpen={Boolean(blockedNavigationPath)}
        onOpenChange={(open) => {
          if (!open) {
            setBlockedNavigationPath(null)
          }
        }}
        title="Leave Listening attempt?"
        footer={
          <>
            <Button onClick={() => setBlockedNavigationPath(null)} variant="secondary">
              Keep working
            </Button>
            <Button onClick={() => leaveListeningAttempt(blockedNavigationPath ?? ROUTES.tests)}>
              Leave anyway
            </Button>
          </>
        }
      >
        <p>Save each changed answer or submit the attempt before leaving if you want the backend to keep it.</p>
      </Dialog>
    </>
  )
}
