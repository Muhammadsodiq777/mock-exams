import { useEffect, useMemo, useState } from 'react'
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
  useCreateWritingSubmission,
  useRetryWritingFeedback,
  useWritingFeedback,
  useWritingSubmission,
} from '../../hooks/useWritingPractice'
import { featureFlags } from '../../services/api'
import type { WritingCriterionFeedbackRDO, WritingFeedbackRDO, WritingTaskType } from '../../types/backend'
import { normalizeApiError, toFormErrorState } from '../../utils/apiErrors'
import { formatDate, formatNumber, humanizeToken } from '../../utils/formatters'

type WritingDraft = {
  answerText: string
  prompt: string
  submissionId?: number | null
  taskType: WritingTaskType
}

type WritingField = 'taskType' | 'prompt' | 'answerText'

const storageKey = 'mock-exams.writing-practice'
const defaultDraft: WritingDraft = {
  answerText: '',
  prompt: '',
  submissionId: null,
  taskType: 'writing_task_2',
}

const taskGuidance: Record<WritingTaskType, { max: number; min: number; name: string }> = {
  writing_task_1: { max: 220, min: 150, name: 'Task 1' },
  writing_task_2: { max: 320, min: 250, name: 'Task 2' },
}

const criteriaOrder = [
  'task_achievement',
  'task_response',
  'coherence_and_cohesion',
  'lexical_resource',
  'grammatical_range_and_accuracy',
] as const

const fieldNames = ['taskType', 'prompt', 'answerText'] as const

const loadDraft = (): WritingDraft => {
  if (typeof window === 'undefined') {
    return defaultDraft
  }

  const rawValue = window.localStorage.getItem(storageKey)
  if (!rawValue) {
    return defaultDraft
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<WritingDraft>
    return {
      answerText: parsed.answerText ?? '',
      prompt: parsed.prompt ?? '',
      submissionId: parsed.submissionId ?? null,
      taskType:
        parsed.taskType === 'writing_task_1' || parsed.taskType === 'writing_task_2'
          ? parsed.taskType
          : defaultDraft.taskType,
    }
  } catch {
    window.localStorage.removeItem(storageKey)
    return defaultDraft
  }
}

const saveDraft = (draft: WritingDraft) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(storageKey, JSON.stringify(draft))
  }
}

const countWords = (value: string) => value.trim().split(/\s+/).filter(Boolean).length

const draftMatchesSubmission = (draft: WritingDraft, submission?: { answerText: string; prompt: string; taskType: WritingTaskType } | null) =>
  Boolean(submission) &&
  submission?.prompt === draft.prompt &&
  submission?.answerText === draft.answerText &&
  submission?.taskType === draft.taskType

const criterionLabel = (key: string, taskType: WritingTaskType) => {
  if (key === 'task_achievement') {
    return taskType === 'writing_task_1' ? 'Task Achievement' : 'Task Achievement'
  }

  if (key === 'task_response') {
    return taskType === 'writing_task_2' ? 'Task Response' : 'Task Response'
  }

  return humanizeToken(key)
}

function CriteriaCard({
  criterion,
  label,
}: {
  criterion?: WritingCriterionFeedbackRDO
  label: string
}) {
  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone={criterion?.band ? 'success' : 'info'}>{label}</Badge>
        <Badge tone="info">Band {formatNumber(criterion?.band, '-')}</Badge>
      </div>
      <p className="feature-card__summary">{criterion?.feedback ?? 'Feedback is not available for this criterion yet.'}</p>
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
  taskType,
}: {
  feedback?: WritingFeedbackRDO | null
  isFetching: boolean
  onRefresh: () => void
  onRetry: () => void
  retrying: boolean
  taskType: WritingTaskType
}) {
  if (!feedback) {
    return (
      <EmptyState
        title="Feedback pending"
        message="Submit an essay to start AI feedback, then refresh or wait for polling."
      />
    )
  }

  if (feedback.status === 'queued' || feedback.status === 'processing') {
    return (
      <div className="feature-panel-state">
        <Loader label={feedback.status === 'queued' ? 'Feedback queued' : 'Feedback processing'} />
        <p className="feature-card__summary">
          The page will keep checking this submission. Refreshing does not create a duplicate essay.
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
      <div className="writing-feedback-summary">
        <RepeatedItemCard>
          <Badge tone="success">Completed</Badge>
          <p className="dashboard-metric">{formatNumber(feedback.overallBand)}</p>
          <DescriptionList
            items={[
              { label: 'Completed', value: formatDate(feedback.completedAt ?? feedback.createdAt) },
              { label: 'Provider', value: feedback.provider ?? 'Not reported' },
              { label: 'Model', value: feedback.model ?? 'Not reported' },
              { label: 'Retry count', value: feedback.retryCount },
            ]}
          />
        </RepeatedItemCard>
      </div>

      <div className="feature-card-grid feature-card-grid--compact">
        {criteriaOrder
          .filter((key) =>
            taskType === 'writing_task_1' ? key !== 'task_response' : key !== 'task_achievement',
          )
          .map((key) => (
            <CriteriaCard
              criterion={feedback.criteriaScores[key]}
              key={key}
              label={criterionLabel(key, taskType)}
            />
          ))}
      </div>

      <Panel title="Mistakes" description="Examples returned by the feedback engine.">
        <div className="feature-panel-body">
          {feedback.mistakes.length ? (
            <div className="feature-list">
              {feedback.mistakes.map((mistake, index) => (
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

      <Panel title="Improved answer" description="A model answer-style rewrite when available.">
        <div className="feature-panel-body">
          {feedback.improvedAnswer ? (
            <pre className="writing-pre">{feedback.improvedAnswer}</pre>
          ) : (
            <EmptyState title="No improved answer" message="The feedback did not include a rewrite." />
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

export function WritingPracticePage() {
  const [draft, setDraft] = useState<WritingDraft>(() => loadDraft())
  const [formErrors, setFormErrors] = useState<Partial<Record<WritingField, string>>>({})
  const [formMessage, setFormMessage] = useState<string | null>(null)
  const createSubmission = useCreateWritingSubmission()
  const submissionQuery = useWritingSubmission(draft.submissionId)
  const wordCount = useMemo(() => countWords(draft.answerText), [draft.answerText])
  const guidance = taskGuidance[draft.taskType]
  const usageError = createSubmission.isError ? normalizeApiError(createSubmission.error) : null
  const hasDraftText = Boolean(draft.prompt.trim() || draft.answerText.trim())
  const submission = submissionQuery.data
  const submittedDraftIsVisible = draftMatchesSubmission(draft, submission)
  const draftChangedSinceSubmission = Boolean(draft.submissionId && submission && !submittedDraftIsVisible)
  const hasPersistedSubmission = Boolean(draft.submissionId && !draftChangedSinceSubmission)
  const currentSubmissionId = hasPersistedSubmission ? draft.submissionId : null
  const feedbackQuery = useWritingFeedback(currentSubmissionId, hasPersistedSubmission)
  const retryFeedback = useRetryWritingFeedback(currentSubmissionId)
  const aiUsageQuery = useAiUsage(featureFlags.ai)
  const providerError = retryFeedback.isError ? normalizeApiError(retryFeedback.error) : null
  const primaryActionLabel = hasPersistedSubmission
    ? feedbackQuery.data?.status === 'failed'
      ? 'Retry feedback'
      : 'Refresh feedback'
    : 'Submit for feedback'

  useEffect(() => {
    saveDraft(draft)
  }, [draft])

  useEffect(() => {
    if (!hasDraftText || submittedDraftIsVisible) {
      return undefined
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasDraftText, submittedDraftIsVisible])

  const updateDraft = <TKey extends keyof WritingDraft>(key: TKey, value: WritingDraft[TKey]) => {
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
  }

  const validate = () => {
    const nextErrors: Partial<Record<WritingField, string>> = {}

    if (!draft.prompt.trim()) {
      nextErrors.prompt = 'Enter the writing prompt before submitting.'
    }

    if (!draft.answerText.trim()) {
      nextErrors.answerText = 'Enter your essay before submitting.'
    }

    setFormErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleSubmit = async () => {
    if (!validate()) {
      setFormMessage('Fix the highlighted fields before submitting.')
      return
    }

    setFormMessage(null)

    if (hasPersistedSubmission) {
      try {
        if (feedbackQuery.data?.status === 'failed') {
          await retryFeedback.mutateAsync()
          return
        }

        await Promise.all([submissionQuery.refetch(), feedbackQuery.refetch()])
      } catch (error) {
        setFormMessage(error instanceof Error ? error.message : 'Feedback could not be refreshed.')
      }
      return
    }

    try {
      const nextSubmission = await createSubmission.mutateAsync({
        answerText: draft.answerText.trim(),
        prompt: draft.prompt.trim(),
        taskType: draft.taskType,
        testAttemptId: null,
      })
      setDraft((current) => ({ ...current, submissionId: nextSubmission.id }))
    } catch (error) {
      const nextFormState = toFormErrorState(error, fieldNames, 'Writing submission could not be created.')
      setFormErrors(nextFormState.fieldErrors)
      setFormMessage(nextFormState.formMessage)
    }
  }

  const handleRetry = async () => {
    await retryFeedback.mutateAsync()
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Practice"
        title="Writing"
        actions={
          <>
            <Link className="feature-link" to={ROUTES.dashboard}>
              Dashboard
            </Link>
            <Link className="feature-link" to={ROUTES.tests}>
              Tests
            </Link>
          </>
        }
      />

      <div className="writing-layout">
        <Panel title="Essay draft" description="Plain-text IELTS writing submission.">
          <div className="feature-panel-body">
            {formMessage && (
              <Alert title="Submission blocked" variant="danger">
                {formMessage}
              </Alert>
            )}
            {usageError?.code === 'AI_USAGE_LIMIT_EXCEEDED' && (
              <Alert title="AI usage limit reached" variant="warning">
                {usageError.message}
              </Alert>
            )}
            {(usageError?.code === 'SERVICE_UNAVAILABLE' || usageError?.code === 'AI_PROVIDER_FAILED') && (
              <Alert title="AI provider unavailable" variant="warning">
                {usageError.message}
              </Alert>
            )}
            <SelectField
              label="Task type"
              onChange={(event) => updateDraft('taskType', event.target.value as WritingTaskType)}
              value={draft.taskType}
            >
              <option value="writing_task_1">Task 1</option>
              <option value="writing_task_2">Task 2</option>
            </SelectField>
            <TextareaField
              error={formErrors.prompt}
              helpText="Paste or type the full IELTS prompt. It is submitted as plain text."
              label="Prompt"
              onChange={(event) => updateDraft('prompt', event.target.value)}
              required
              rows={5}
              value={draft.prompt}
            />
            <TextareaField
              error={formErrors.answerText}
              helpText={`${guidance.name} guidance: at least ${guidance.min} words; aim to stay near ${guidance.max} words.`}
              label="Essay"
              onChange={(event) => updateDraft('answerText', event.target.value)}
              required
              rows={16}
              value={draft.answerText}
            />
            <div className="writing-editor-meta">
              <Badge tone={wordCount >= guidance.min ? 'success' : 'warning'}>{wordCount} words</Badge>
              <Badge tone={wordCount > guidance.max ? 'warning' : 'info'}>
                {guidance.min}-{guidance.max} target
              </Badge>
              {hasPersistedSubmission ? (
                <Badge tone={submittedDraftIsVisible ? 'success' : 'info'}>Submission {draft.submissionId}</Badge>
              ) : hasDraftText ? (
                <Badge tone="warning">Unsubmitted draft</Badge>
              ) : null}
            </div>
            <div className="feature-card__actions">
              <Button
                disabled={
                  createSubmission.isPending ||
                  retryFeedback.isPending ||
                  (hasPersistedSubmission && (submissionQuery.isFetching || feedbackQuery.isFetching))
                }
                isLoading={
                  createSubmission.isPending ||
                  (hasPersistedSubmission && (retryFeedback.isPending || feedbackQuery.isFetching))
                }
                loadingLabel={hasPersistedSubmission ? 'Checking' : 'Submitting'}
                onClick={handleSubmit}
              >
                {primaryActionLabel}
              </Button>
              <Button
                disabled={!draft.submissionId || feedbackQuery.isFetching}
                isLoading={feedbackQuery.isFetching}
                loadingLabel="Refreshing"
                onClick={() => void feedbackQuery.refetch()}
                variant="secondary"
              >
                Refresh feedback
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
                  <Badge tone="info">{humanizeToken(submission.taskType)}</Badge>
                  <DescriptionList
                    items={[
                      { label: 'Submitted', value: formatDate(submission.createdAt) },
                      { label: 'Words', value: countWords(submission.answerText) },
                      { label: 'Prompt', value: submission.prompt },
                    ]}
                  />
                </RepeatedItemCard>
              ) : (
                <EmptyState title="No submission yet" message="Submit an essay to track feedback here." />
              )}
            </div>
          </Panel>
        </aside>
      </div>

      <Panel title="AI feedback" description="Queued, processing, failed, and completed feedback states.">
        <div className="feature-panel-body">
          {providerError && (
            <Alert title="Retry failed" variant="danger">
              {providerError.code === 'AI_USAGE_LIMIT_EXCEEDED'
                ? providerError.message
                : providerError.code === 'SERVICE_UNAVAILABLE' || providerError.code === 'AI_PROVIDER_FAILED'
                  ? providerError.message
                  : providerError.message}
            </Alert>
          )}
          {feedbackQuery.isLoading && draft.submissionId ? (
            <Loader label="Loading feedback" />
          ) : feedbackQuery.isError ? (
            <Alert title="Feedback could not be loaded" variant="warning">
              {feedbackQuery.error instanceof Error
                ? feedbackQuery.error.message
                : 'Refresh feedback after the backend starts processing.'}
            </Alert>
          ) : (
            <FeedbackPanel
              feedback={feedbackQuery.data}
              isFetching={feedbackQuery.isFetching}
              onRefresh={() => void feedbackQuery.refetch()}
              onRetry={() => void handleRetry()}
              retrying={retryFeedback.isPending}
              taskType={submission?.taskType ?? draft.taskType}
            />
          )}
        </div>
      </Panel>
    </PageContainer>
  )
}
