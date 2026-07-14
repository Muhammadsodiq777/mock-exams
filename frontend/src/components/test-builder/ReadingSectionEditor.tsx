import { useMemo, useState } from 'react'

import { Alert, Badge, Button, EmptyState, RepeatedItemCard, TextField } from '../ui'
import type {
  ApiFieldError,
  BackendId,
  TeacherQuestionCDO,
  TeacherQuestionRDO,
  TeacherReadingParagraphCDO,
  TeacherReadingParagraphRDO,
  TeacherTestSectionRDO,
} from '../../types/backend'
import { toPlainRichTextValue } from '../../utils/richText'
import { QuestionEditor } from './QuestionEditor'
import { RichTextPlainEditor } from './RichTextPlainEditor'

type ParagraphDraft = {
  content: string
  label: string
}

type ReadingSectionEditorProps = {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isParagraphBusy: boolean
  isQuestionBusy: boolean
  onAddParagraph: (payload: TeacherReadingParagraphCDO) => Promise<void>
  onAddQuestion: (payload: TeacherQuestionCDO) => Promise<void>
  onDeleteParagraph: (paragraphId: BackendId) => Promise<void>
  onDeleteQuestion: (questionId: BackendId) => Promise<void>
  onReorderParagraphs: (paragraphIds: BackendId[]) => Promise<void>
  onUpdateParagraph: (paragraphId: BackendId, payload: TeacherReadingParagraphCDO) => Promise<void>
  onUpdateQuestion: (questionId: BackendId, payload: TeacherQuestionCDO) => Promise<void>
  section: TeacherTestSectionRDO
}

const optionalString = (value: string) => {
  const trimmed = value.trim()
  return trimmed || null
}

const nextParagraphLabel = (paragraphs: TeacherReadingParagraphRDO[]) => {
  const existing = new Set(paragraphs.map((paragraph) => paragraph.label?.trim()).filter(Boolean))
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
  return alphabet.find((label) => !existing.has(label)) ?? ''
}

const orderedParagraphs = (section: TeacherTestSectionRDO) =>
  [...(section.paragraphs ?? [])].sort((left, right) => left.paragraphOrder - right.paragraphOrder)

const orderedQuestions = (section: TeacherTestSectionRDO) =>
  [...(section.questions ?? [])].sort((left, right) => left.questionOrder - right.questionOrder)

const paragraphDraftFromParagraph = (paragraph?: TeacherReadingParagraphRDO): ParagraphDraft => ({
  content: paragraph?.contentText ?? paragraph?.content ?? '',
  label: paragraph?.label ?? '',
})

const paragraphPayload = (draft: ParagraphDraft): TeacherReadingParagraphCDO => {
  const content = toPlainRichTextValue(draft.content)
  return {
    content: optionalString(content.text),
    contentDocument: content.document,
    label: optionalString(draft.label),
  }
}

const fieldError = (fieldErrors: ApiFieldError[], candidates: string[]) =>
  fieldErrors.find((error) =>
    candidates.some((candidate) => error.field === candidate || error.field.endsWith(`.${candidate}`)),
  )?.message

function ParagraphEditor({
  disabled,
  fieldErrors,
  isBusy,
  onDelete,
  onMove,
  onSave,
  paragraph,
  position,
  total,
}: {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isBusy: boolean
  onDelete: (paragraphId: BackendId) => Promise<void>
  onMove: (paragraphId: BackendId, direction: -1 | 1) => Promise<void>
  onSave: (paragraphId: BackendId, payload: TeacherReadingParagraphCDO) => Promise<void>
  paragraph: TeacherReadingParagraphRDO
  position: number
  total: number
}) {
  const [draft, setDraft] = useState<ParagraphDraft>(() => paragraphDraftFromParagraph(paragraph))

  return (
    <RepeatedItemCard className="test-builder-paragraph-editor">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">Paragraph {paragraph.paragraphOrder}</p>
          <h4>{paragraph.label ? `Label ${paragraph.label}` : 'Unlabelled paragraph'}</h4>
        </div>
        <div className="test-builder-actions">
          <Button
            aria-label="Move paragraph up"
            disabled={disabled || position === 0}
            onClick={() => void onMove(paragraph.id, -1).catch(() => undefined)}
            size="sm"
            variant="ghost"
          >
            Up
          </Button>
          <Button
            aria-label="Move paragraph down"
            disabled={disabled || position === total - 1}
            onClick={() => void onMove(paragraph.id, 1).catch(() => undefined)}
            size="sm"
            variant="ghost"
          >
            Down
          </Button>
          <Button
            disabled={disabled}
            onClick={() => void onDelete(paragraph.id).catch(() => undefined)}
            size="sm"
            variant="danger"
          >
            Delete
          </Button>
        </div>
      </div>
      <TextField
        disabled={disabled}
        error={fieldError(fieldErrors, ['label'])}
        label="Paragraph label"
        maxLength={20}
        onChange={(event) => setDraft((current) => ({ ...current, label: event.target.value }))}
        value={draft.label}
      />
      <RichTextPlainEditor
        backendContent={
          paragraph.renderedHtml || paragraph.contentText
            ? {
                contentDocument: paragraph.contentDocument,
                contentText: paragraph.contentText,
                renderedHtml: paragraph.renderedHtml,
              }
            : undefined
        }
        disabled={disabled}
        error={fieldError(fieldErrors, ['content', 'contentDocument'])}
        label="Paragraph content"
        onChange={(value) => setDraft((current) => ({ ...current, content: value.text }))}
        required
        value={draft.content}
      />
      <div className="test-builder-actions">
        <Button
          disabled={disabled}
          isLoading={isBusy}
          loadingLabel="Saving"
          onClick={() => void onSave(paragraph.id, paragraphPayload(draft)).catch(() => undefined)}
        >
          Save paragraph
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

function NewParagraphEditor({
  disabled,
  fieldErrors,
  isBusy,
  onAdd,
  paragraphs,
}: {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isBusy: boolean
  onAdd: (payload: TeacherReadingParagraphCDO) => Promise<void>
  paragraphs: TeacherReadingParagraphRDO[]
}) {
  const [draft, setDraft] = useState<ParagraphDraft>(() => ({
    content: '',
    label: nextParagraphLabel(paragraphs),
  }))

  const add = async () => {
    await onAdd(paragraphPayload(draft))
    setDraft({ content: '', label: '' })
  }

  return (
    <RepeatedItemCard className="test-builder-paragraph-editor">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">New paragraph</p>
          <h4>Add passage paragraph</h4>
        </div>
      </div>
      <TextField
        disabled={disabled}
        error={fieldError(fieldErrors, ['label'])}
        label="Paragraph label"
        maxLength={20}
        onChange={(event) => setDraft((current) => ({ ...current, label: event.target.value }))}
        value={draft.label}
      />
      <RichTextPlainEditor
        disabled={disabled}
        error={fieldError(fieldErrors, ['content', 'contentDocument'])}
        helpText="Backend-owned file ids for passage images, diagrams, or documents should stay as file-id notes until rich embed toolbar support is added."
        label="Paragraph content"
        onChange={(value) => setDraft((current) => ({ ...current, content: value.text }))}
        required
        value={draft.content}
      />
      <div className="test-builder-actions">
        <Button
          disabled={disabled}
          isLoading={isBusy}
          loadingLabel="Adding"
          onClick={() => void add().catch(() => undefined)}
        >
          Add paragraph
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

export function ReadingSectionEditor({
  disabled,
  fieldErrors,
  isParagraphBusy,
  isQuestionBusy,
  onAddParagraph,
  onAddQuestion,
  onDeleteParagraph,
  onDeleteQuestion,
  onReorderParagraphs,
  onUpdateParagraph,
  onUpdateQuestion,
  section,
}: ReadingSectionEditorProps) {
  const paragraphs = useMemo(() => orderedParagraphs(section), [section])
  const questions = useMemo(() => orderedQuestions(section), [section])
  const paragraphLabels = useMemo(
    () =>
      paragraphs
        .map((paragraph) => paragraph.label?.trim())
        .filter((label): label is string => Boolean(label)),
    [paragraphs],
  )

  const moveParagraph = async (paragraphId: BackendId, direction: -1 | 1) => {
    const currentIndex = paragraphs.findIndex((paragraph) => paragraph.id === paragraphId)
    const targetIndex = currentIndex + direction
    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= paragraphs.length) {
      return
    }

    const nextIds = paragraphs.map((paragraph) => paragraph.id)
    const [moved] = nextIds.splice(currentIndex, 1)
    nextIds.splice(targetIndex, 0, moved)
    await onReorderParagraphs(nextIds)
  }

  return (
    <div className="test-builder-reading-editor">
      <Alert title="Reading content" variant="info">
        Saved paragraph labels are the only choices available when linking questions to passage
        paragraphs. External URLs are not accepted for passage embeds; use backend file ids only.
      </Alert>

      <section className="test-builder-reading-editor__section">
        <div className="test-builder-editor-heading">
          <div>
            <p className="test-builder-muted">Passage outline</p>
            <h3>Paragraphs</h3>
          </div>
          <Badge tone={paragraphs.length ? 'success' : 'warning'}>
            {paragraphs.length} paragraph{paragraphs.length === 1 ? '' : 's'}
          </Badge>
        </div>
        {paragraphs.length === 0 ? (
          <EmptyState
            eyebrow="No paragraphs"
            message="Add labelled or unlabelled passage paragraphs. If no paragraph rows exist, preview falls back to the section passage."
            title="Build the passage outline"
          />
        ) : (
          paragraphs.map((paragraph, index) => (
            <ParagraphEditor
              disabled={disabled}
              fieldErrors={fieldErrors}
              isBusy={isParagraphBusy}
              key={paragraph.id}
              onDelete={async (paragraphId) => {
                const confirmed = window.confirm('Delete this paragraph? Related question links may need updates.')
                if (confirmed) {
                  await onDeleteParagraph(paragraphId)
                }
              }}
              onMove={moveParagraph}
              onSave={onUpdateParagraph}
              paragraph={paragraph}
              position={index}
              total={paragraphs.length}
            />
          ))
        )}
        <NewParagraphEditor
          disabled={disabled}
          fieldErrors={fieldErrors}
          isBusy={isParagraphBusy}
          onAdd={onAddParagraph}
          paragraphs={paragraphs}
        />
      </section>

      <section className="test-builder-reading-editor__section">
        <div className="test-builder-editor-heading">
          <div>
            <p className="test-builder-muted">Question set</p>
            <h3>Questions</h3>
          </div>
          <Badge tone={questions.length ? 'success' : 'warning'}>
            {questions.length} question{questions.length === 1 ? '' : 's'}
          </Badge>
        </div>
        {questions.length === 0 && (
          <EmptyState
            eyebrow="No questions"
            message="Add objective questions with answer keys, or subjective response questions without answer keys."
            title="Create the first question"
          />
        )}
        {questions.map((question: TeacherQuestionRDO) => (
          <QuestionEditor
            disabled={disabled}
            fieldErrors={fieldErrors}
            isBusy={isQuestionBusy}
            key={question.id}
            onDelete={async (questionId) => {
              const confirmed = window.confirm('Delete this question?')
              if (confirmed) {
                await onDeleteQuestion(questionId)
              }
            }}
            onSave={(payload) => onUpdateQuestion(question.id, payload)}
            paragraphLabels={paragraphLabels}
            question={question}
          />
        ))}
        <QuestionEditor
          disabled={disabled}
          fieldErrors={fieldErrors}
          isBusy={isQuestionBusy}
          onSave={onAddQuestion}
          paragraphLabels={paragraphLabels}
        />
      </section>
    </div>
  )
}
