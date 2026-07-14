import { useMemo, useState } from 'react'

import { Alert, Button, RepeatedItemCard, SelectField, TextField } from '../ui'
import type {
  ApiFieldError,
  BackendId,
  TeacherQuestionAnswerCDO,
  TeacherQuestionCDO,
  TeacherQuestionOptionCDO,
  TeacherQuestionRDO,
} from '../../types/backend'
import { humanizeToken } from '../../utils/formatters'
import { toPlainRichTextValue } from '../../utils/richText'
import { RichTextPlainEditor } from './RichTextPlainEditor'

type QuestionOptionDraft = {
  optionLabel: string
  optionText: string
}

type QuestionAnswerDraft = {
  answerValue: string
  explanation: string
  primary: boolean
}

type QuestionDraft = {
  answers: QuestionAnswerDraft[]
  instructions: string
  listeningSegmentId: string
  options: QuestionOptionDraft[]
  questionText: string
  questionType: string
  relatedParagraphLabel: string
  score: string
}

type QuestionSegmentOption = {
  id: BackendId
  label: string
}

type QuestionTypeDefinition = {
  answerChoices?: string[]
  objective: boolean
  usesOptions: boolean
  value: string
}

type QuestionEditorProps = {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isBusy?: boolean
  listeningSegments?: QuestionSegmentOption[]
  onDelete?: (questionId: BackendId) => Promise<void>
  onSave: (payload: TeacherQuestionCDO, questionId?: BackendId) => Promise<void>
  paragraphLabels: string[]
  question?: TeacherQuestionRDO
}

const questionTypes: QuestionTypeDefinition[] = [
  { objective: true, usesOptions: true, value: 'multiple_choice' },
  { objective: true, usesOptions: true, value: 'matching_information' },
  { objective: true, usesOptions: false, value: 'true_false_not_given', answerChoices: ['TRUE', 'FALSE', 'NOT GIVEN'] },
  { objective: true, usesOptions: false, value: 'yes_no_not_given', answerChoices: ['YES', 'NO', 'NOT GIVEN'] },
  { objective: true, usesOptions: false, value: 'sentence_completion' },
  { objective: true, usesOptions: false, value: 'summary_completion' },
  { objective: true, usesOptions: false, value: 'table_completion' },
  { objective: true, usesOptions: false, value: 'diagram_labeling' },
  { objective: true, usesOptions: false, value: 'short_answer' },
  { objective: true, usesOptions: false, value: 'gap_fill' },
  { objective: true, usesOptions: true, value: 'ordering' },
  { objective: false, usesOptions: false, value: 'essay_answer' },
  { objective: false, usesOptions: false, value: 'audio_answer' },
]

const defaultQuestionType = questionTypes[0].value

const optionLabels = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

const optionalString = (value: string) => {
  const trimmed = value.trim()
  return trimmed || null
}

const optionalNumber = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? Number(trimmed) : null
}

const questionDefinition = (questionType: string) =>
  questionTypes.find((type) => type.value === questionType) ?? questionTypes[0]

const defaultOptions = (): QuestionOptionDraft[] =>
  optionLabels.slice(0, 4).map((label) => ({ optionLabel: label, optionText: '' }))

const defaultAnswers = (questionType: string): QuestionAnswerDraft[] =>
  questionDefinition(questionType).objective
    ? [{ answerValue: '', explanation: '', primary: true }]
    : []

const draftFromQuestion = (question?: TeacherQuestionRDO): QuestionDraft => {
  const questionType = question?.questionType ?? defaultQuestionType
  const definition = questionDefinition(questionType)

  return {
    answers: definition.objective
      ? [...(question?.answerKeys ?? [])]
          .sort((left, right) => left.answerOrder - right.answerOrder)
          .map((answer) => ({
            answerValue: answer.answerValue ?? '',
            explanation: answer.explanationContent?.contentText ?? answer.explanation ?? '',
            primary: answer.primary,
          }))
      : [],
    instructions: question?.instructionsContent?.contentText ?? question?.instructions ?? '',
    listeningSegmentId: question?.listeningSegmentId ? String(question.listeningSegmentId) : '',
    options: definition.usesOptions
      ? (question?.options?.length
          ? [...question.options]
              .sort((left, right) => left.optionOrder - right.optionOrder)
              .map((option) => ({
                optionLabel: option.optionLabel ?? '',
                optionText: option.optionTextContent?.contentText ?? option.optionText ?? '',
              }))
          : defaultOptions())
      : [],
    questionText: question?.questionTextContent?.contentText ?? question?.questionText ?? '',
    questionType,
    relatedParagraphLabel: question?.relatedParagraphLabel ?? '',
    score: question?.score === undefined || question?.score === null ? '1' : String(question.score),
  }
}

const fieldError = (fieldErrors: ApiFieldError[], candidates: string[]) =>
  fieldErrors.find((error) =>
    candidates.some((candidate) => error.field === candidate || error.field.endsWith(`.${candidate}`)),
  )?.message

const toQuestionPayload = (draft: QuestionDraft, listeningSegments?: QuestionSegmentOption[]): TeacherQuestionCDO => {
  const prompt = toPlainRichTextValue(draft.questionText)
  const instructions = toPlainRichTextValue(draft.instructions)
  const definition = questionDefinition(draft.questionType)
  const parsedSegmentId = optionalNumber(draft.listeningSegmentId)
  const allowedSegmentIds = listeningSegments
    ? new Set(listeningSegments.map((segment) => segment.id))
    : null
  const listeningSegmentId =
    parsedSegmentId && (!allowedSegmentIds || allowedSegmentIds.has(parsedSegmentId))
      ? parsedSegmentId
      : null
  const options: TeacherQuestionOptionCDO[] | null = definition.usesOptions
    ? draft.options.map((option) => {
        const optionContent = toPlainRichTextValue(option.optionText)
        return {
          optionDocument: optionContent.document,
          optionLabel: option.optionLabel.trim(),
          optionText: optionalString(optionContent.text),
        }
      })
    : null
  const answers: TeacherQuestionAnswerCDO[] | null = definition.objective
    ? draft.answers.map((answer, index) => {
        const explanation = toPlainRichTextValue(answer.explanation)
        return {
          answerValue: optionalString(answer.answerValue),
          explanation: optionalString(explanation.text),
          explanationDocument: explanation.document,
          primary: index === 0 ? true : answer.primary,
        }
      })
    : null

  return {
    answers,
    instructions: optionalString(instructions.text),
    instructionsDocument: instructions.document,
    listeningSegmentId,
    options,
    questionDocument: prompt.document,
    questionText: optionalString(prompt.text),
    questionType: draft.questionType,
    relatedParagraphLabel: optionalString(draft.relatedParagraphLabel),
    score: optionalNumber(draft.score),
  }
}

function AnswerValueField({
  answer,
  choices,
  disabled,
  error,
  index,
  onChange,
  optionLabelsForSelect,
}: {
  answer: QuestionAnswerDraft
  choices?: string[]
  disabled: boolean
  error?: string
  index: number
  onChange: (value: string) => void
  optionLabelsForSelect: string[]
}) {
  const selectChoices = choices ?? optionLabelsForSelect

  if (selectChoices.length > 0) {
    return (
      <SelectField
        disabled={disabled}
        error={error}
        label={index === 0 ? 'Primary answer' : 'Accepted answer'}
        onChange={(event) => onChange(event.target.value)}
        value={answer.answerValue}
      >
        <option value="">Choose answer</option>
        {selectChoices.map((choice) => (
          <option key={choice} value={choice}>
            {choice}
          </option>
        ))}
      </SelectField>
    )
  }

  return (
    <TextField
      disabled={disabled}
      error={error}
      label={index === 0 ? 'Primary answer' : 'Accepted answer'}
      onChange={(event) => onChange(event.target.value)}
      value={answer.answerValue}
    />
  )
}

export function QuestionEditor({
  disabled,
  fieldErrors,
  isBusy = false,
  listeningSegments,
  onDelete,
  onSave,
  paragraphLabels,
  question,
}: QuestionEditorProps) {
  const [draft, setDraft] = useState<QuestionDraft>(() => draftFromQuestion(question))
  const definition = questionDefinition(draft.questionType)
  const savedOptions = useMemo(
    () =>
      [...(question?.options ?? [])].sort((left, right) => left.optionOrder - right.optionOrder),
    [question],
  )
  const savedAnswerKeys = useMemo(
    () =>
      [...(question?.answerKeys ?? [])].sort((left, right) => left.answerOrder - right.answerOrder),
    [question],
  )
  const optionLabelsForSelect = useMemo(
    () =>
      draft.options
        .map((option) => option.optionLabel.trim())
        .filter(Boolean),
    [draft.options],
  )
  const title = question ? `Question ${question.questionOrder}` : 'New question'

  const update = <TKey extends keyof QuestionDraft>(key: TKey, value: QuestionDraft[TKey]) => {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  const updateType = (questionType: string) => {
    const nextDefinition = questionDefinition(questionType)
    setDraft((current) => ({
      ...current,
      answers: nextDefinition.objective ? current.answers.length ? current.answers : defaultAnswers(questionType) : [],
      options: nextDefinition.usesOptions ? current.options.length ? current.options : defaultOptions() : [],
      questionType,
    }))
  }

  const updateOption = (index: number, next: Partial<QuestionOptionDraft>) => {
    update(
      'options',
      draft.options.map((option, optionIndex) =>
        optionIndex === index ? { ...option, ...next } : option,
      ),
    )
  }

  const updateAnswer = (index: number, next: Partial<QuestionAnswerDraft>) => {
    update(
      'answers',
      draft.answers.map((answer, answerIndex) =>
        answerIndex === index ? { ...answer, ...next } : answer,
      ),
    )
  }

  const save = async () => {
    await onSave(toQuestionPayload(draft, listeningSegments), question?.id)
    if (!question) {
      setDraft(draftFromQuestion())
    }
  }

  return (
    <RepeatedItemCard className="test-builder-question-editor">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">{title}</p>
          <h4>{humanizeToken(draft.questionType)}</h4>
        </div>
        {question && onDelete && (
          <Button
            disabled={disabled}
            onClick={() => void onDelete(question.id).catch(() => undefined)}
            size="sm"
            variant="danger"
          >
            Delete
          </Button>
        )}
      </div>

      <div className="test-builder-form-grid">
        <SelectField
          disabled={disabled}
          error={fieldError(fieldErrors, ['questionType'])}
          label="Question type"
          onChange={(event) => updateType(event.target.value)}
          value={draft.questionType}
        >
          {questionTypes.map((type) => (
            <option key={type.value} value={type.value}>
              {humanizeToken(type.value)}
            </option>
          ))}
        </SelectField>
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['score'])}
          inputMode="decimal"
          label="Score"
          onChange={(event) => update('score', event.target.value)}
          value={draft.score}
        />
        {(paragraphLabels.length > 0 || draft.relatedParagraphLabel) && (
          <SelectField
            disabled={disabled || paragraphLabels.length === 0}
            error={fieldError(fieldErrors, ['relatedParagraphLabel'])}
            helpText={paragraphLabels.length ? undefined : 'Add a labelled paragraph before linking a question.'}
            label="Related paragraph"
            onChange={(event) => update('relatedParagraphLabel', event.target.value)}
            value={draft.relatedParagraphLabel}
          >
            <option value="">No paragraph link</option>
            {paragraphLabels.map((label) => (
              <option key={label} value={label}>
                {label}
              </option>
            ))}
          </SelectField>
        )}
        {listeningSegments && (
          <SelectField
            disabled={disabled || listeningSegments.length === 0}
            error={fieldError(fieldErrors, ['listeningSegmentId'])}
            helpText={listeningSegments.length ? undefined : 'Add a saved listening segment before linking a question.'}
            label="Listening segment"
            onChange={(event) => update('listeningSegmentId', event.target.value)}
            value={draft.listeningSegmentId}
          >
            <option value="">No segment link</option>
            {listeningSegments.map((segment) => (
              <option key={segment.id} value={segment.id}>
                {segment.label}
              </option>
            ))}
          </SelectField>
        )}
      </div>

      <RichTextPlainEditor
        backendContent={question?.questionTextContent}
        disabled={disabled}
        error={fieldError(fieldErrors, ['questionText', 'questionDocument'])}
        label="Prompt"
        onChange={(value) => update('questionText', value.text)}
        required
        value={draft.questionText}
      />
      <RichTextPlainEditor
        backendContent={question?.instructionsContent}
        disabled={disabled}
        error={fieldError(fieldErrors, ['instructions', 'instructionsDocument'])}
        label="Question instructions"
        onChange={(value) => update('instructions', value.text)}
        value={draft.instructions}
      />

      {definition.usesOptions && (
        <div className="test-builder-nested-editor">
          <div className="test-builder-editor-heading">
            <div>
              <p className="test-builder-muted">Options</p>
              <h4>Answer choices</h4>
            </div>
            <Button
              disabled={disabled}
              onClick={() =>
                update('options', [
                  ...draft.options,
                  { optionLabel: optionLabels[draft.options.length] ?? String(draft.options.length + 1), optionText: '' },
                ])
              }
              size="sm"
              variant="secondary"
            >
              Add option
            </Button>
          </div>
          {draft.options.map((option, index) => (
            <div className="test-builder-option-row" key={`${option.optionLabel}-${index}`}>
              <TextField
                disabled={disabled}
                error={fieldError(fieldErrors, ['optionLabel', `options[${index}].optionLabel`])}
                label="Label"
                maxLength={12}
                onChange={(event) => updateOption(index, { optionLabel: event.target.value.toUpperCase() })}
                value={option.optionLabel}
              />
              <RichTextPlainEditor
                backendContent={savedOptions[index]?.optionTextContent}
                disabled={disabled}
                error={fieldError(fieldErrors, ['optionText', `options[${index}].optionText`])}
                label="Option text"
                onChange={(value) => updateOption(index, { optionText: value.text })}
                required
                value={option.optionText}
              />
              <Button
                disabled={disabled || draft.options.length <= 1}
                onClick={() => update('options', draft.options.filter((_, optionIndex) => optionIndex !== index))}
                size="sm"
                variant="ghost"
              >
                Remove
              </Button>
            </div>
          ))}
        </div>
      )}

      {definition.objective ? (
        <div className="test-builder-nested-editor">
          <div className="test-builder-editor-heading">
            <div>
              <p className="test-builder-muted">Answer key</p>
              <h4>Accepted answers</h4>
            </div>
            <Button
              disabled={disabled}
              onClick={() => update('answers', [...draft.answers, { answerValue: '', explanation: '', primary: false }])}
              size="sm"
              variant="secondary"
            >
              Add accepted answer
            </Button>
          </div>
          {draft.answers.length === 0 && (
            <Alert title="Answer key missing" variant="warning">
              Objective question types need at least one answer key before the backend will accept them.
            </Alert>
          )}
          {draft.answers.map((answer, index) => (
            <div className="test-builder-answer-row" key={index}>
              <AnswerValueField
                answer={answer}
                choices={definition.answerChoices}
                disabled={disabled}
                error={fieldError(fieldErrors, ['answerValue', `answers[${index}].answerValue`])}
                index={index}
                onChange={(answerValue) => updateAnswer(index, { answerValue })}
                optionLabelsForSelect={definition.usesOptions && draft.questionType !== 'ordering' ? optionLabelsForSelect : []}
              />
              <label className="test-builder-checkbox">
                <input
                  checked={index === 0 || answer.primary}
                  disabled={disabled || index === 0}
                  onChange={(event) => updateAnswer(index, { primary: event.target.checked })}
                  type="checkbox"
                />
                <span>Primary</span>
              </label>
              <RichTextPlainEditor
                backendContent={savedAnswerKeys[index]?.explanationContent}
                disabled={disabled}
                error={fieldError(fieldErrors, ['explanation', `answers[${index}].explanation`])}
                label="Explanation"
                onChange={(value) => updateAnswer(index, { explanation: value.text })}
                value={answer.explanation}
              />
              <Button
                disabled={disabled || draft.answers.length <= 1}
                onClick={() => update('answers', draft.answers.filter((_, answerIndex) => answerIndex !== index))}
                size="sm"
                variant="ghost"
              >
                Remove
              </Button>
            </div>
          ))}
        </div>
      ) : (
        <Alert title="No answer key required" variant="info">
          Essay and audio-like response questions can be saved without objective answer keys.
        </Alert>
      )}

      <div className="test-builder-actions">
        <Button
          disabled={disabled}
          isLoading={isBusy}
          loadingLabel="Saving"
          onClick={() => void save().catch(() => undefined)}
        >
          {question ? 'Save question' : 'Add question'}
        </Button>
      </div>
    </RepeatedItemCard>
  )
}
