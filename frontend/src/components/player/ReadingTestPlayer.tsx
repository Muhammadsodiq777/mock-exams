import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ROUTES, fillRoute } from '../../constants/routes'
import {
  useAttemptResult,
  useSaveAnswer,
  useStartAttempt,
  useSubmitAttempt,
} from '../../hooks/useAttemptLifecycle'
import type {
  BackendId,
  RichTextContentRDO,
  StudentAnswerUDO,
  StudentQuestionRDO,
  StudentTestAttemptRDO,
  TestDetailRDO,
  TestDetailSectionRDO,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatMinutes, formatSeconds, humanizeToken } from '../../utils/formatters'
import {
  Alert,
  Badge,
  Button,
  Dialog,
  EmptyState,
  PlayerLayout,
  RichContent,
  SplitPane,
} from '../ui'

type DraftState = {
  answers: Record<string, string>
  attempt: StudentTestAttemptRDO | null
  persistedAnswers: Record<string, string>
  testId: BackendId
}

const storageKey = (testId: BackendId) => `mock-exams.reading-attempt.${testId}`
const emptyDraft = (testId: BackendId): DraftState => ({
  answers: {},
  attempt: null,
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
          answers: parsed.answers ?? {},
          attempt: parsed.attempt ?? null,
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

const allQuestions = (sections: TestDetailSectionRDO[]) => sections.flatMap(sectionQuestions)
const questionKey = (questionId: BackendId) => String(questionId)

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

export function ReadingTestPlayer({ test }: { test: TestDetailRDO }) {
  const navigate = useNavigate()
  const sections = useMemo(() => orderedSections(test), [test])
  const questions = useMemo(() => allQuestions(sections), [sections])
  const [draft, setDraft] = useState<DraftState>(() => loadDraft(test.id))
  const [activeSectionId, setActiveSectionId] = useState(sections[0]?.id)
  const [now, setNow] = useState(() => Date.now())
  const [savingQuestionId, setSavingQuestionId] = useState<BackendId | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitOpen, setSubmitOpen] = useState(false)
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
  const isAttemptFinalizing = Boolean(
    draft.attempt &&
      isSubmitted &&
      !resultQuery.data?.id &&
      (resultQuery.isLoading || resultQuery.isFetching || resultError),
  )
  const dirtyAnswerQuestions = questions.filter((question) => {
    const value = getDraftAnswer(draft, question.id)
    return Boolean(value) && value !== getPersistedAnswer(draft, question.id)
  })
  const clearedPersistedQuestions = questions.filter((question) => {
    const value = getDraftAnswer(draft, question.id)
    return !value && hasPersistedAnswer(draft, question.id) && getPersistedAnswer(draft, question.id)
  })
  const pendingSaveCount = dirtyAnswerQuestions.length
  const isSubmitting = submitAttempt.isPending || saveAnswer.isPending

  useEffect(() => {
    saveDraft(draft)
  }, [draft])

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [])

  useEffect(() => {
    if (resultQuery.data?.id) {
      clearDraft(test.id)
      navigate(fillRoute(ROUTES.resultDetail, { id: resultQuery.data.id }))
    }
  }, [navigate, resultQuery.data?.id, test.id])

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

  const handleStart = async () => {
    const attempt = await startAttempt.mutateAsync()
    setDraft((current) => ({ ...current, attempt }))
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

  if (sections.length === 0 || questions.length === 0) {
    return (
      <EmptyState
        title="Reading test is not ready"
        message="This published test has no readable sections or questions yet."
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
          Start creates a timed backend attempt. Draft answers are stored locally in this browser while
          each saved answer is sent to the backend one question at a time.
        </p>
        {startAttempt.isError && (
          <Alert title="Attempt could not start" variant="danger">
            {startAttempt.error instanceof Error
              ? startAttempt.error.message
              : 'Log in as a student or check access to this test.'}
          </Alert>
        )}
        <Button isLoading={startAttempt.isPending} loadingLabel="Starting" onClick={handleStart}>
          Start Reading test
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
      <div className="reading-section-nav">
        {sections.map((section) => (
          <button
            className={section.id === activeSection.id ? 'active' : undefined}
            key={section.id}
            onClick={() => setActiveSectionId(section.id)}
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
                  setActiveSectionId(section.id)
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

  const passage = (
    <div className="reading-passage">
      <h2>{activeSection.title}</h2>
      {activeSection.instructionsContent?.renderedHtml ? (
        <RichContent trustedHtml={activeSection.instructionsContent.renderedHtml} />
      ) : (
        activeSection.instructions && <RichContent blocks={[{ text: activeSection.instructions, type: 'paragraph' }]} />
      )}
      {activeSection.paragraphs?.length ? (
        [...activeSection.paragraphs]
          .sort((a, b) => a.paragraphOrder - b.paragraphOrder)
          .map((paragraph) => (
            <section className="reading-paragraph" key={paragraph.id}>
              {paragraph.label && <Badge tone="info">{paragraph.label}</Badge>}
              {paragraph.renderedHtml ? (
                <RichContent trustedHtml={paragraph.renderedHtml} />
              ) : (
                <RichContent blocks={[{ text: paragraph.contentText ?? paragraph.content ?? '', type: 'paragraph' }]} />
              )}
            </section>
          ))
      ) : activeSection.passageTextContent?.renderedHtml ? (
        <RichContent trustedHtml={activeSection.passageTextContent.renderedHtml} />
      ) : (
        <RichContent blocks={[{ text: activeSection.passageText ?? 'No passage text provided.', type: 'paragraph' }]} />
      )}
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

        return (
          <article className="reading-question" id={`question-${question.id}`} key={question.id}>
            <div className="feature-card__badges">
              <Badge tone={value ? 'success' : 'warning'}>
                Question {question.questionOrder}
              </Badge>
              <Badge tone="info">{humanizeToken(question.questionType)}</Badge>
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
            <Link className="feature-link" to={ROUTES.tests}>
              Exit test
            </Link>
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
        <SplitPane left={passage} leftLabel="Reading passage" right={workspace} rightLabel="Questions" />
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
        title="Submit Reading attempt?"
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
          Submitted attempts cannot be edited. The backend will score objective answers and expose
          answer keys only on the result page.
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
    </>
  )
}
