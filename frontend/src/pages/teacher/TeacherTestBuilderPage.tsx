import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  SelectField,
  Skeleton,
  TextField,
  TextareaField,
} from '../../components/ui'
import {
  BuilderPreviewPanel,
  type BuilderPreviewMode,
} from '../../components/test-builder/BuilderPreviewPanel'
import { buildBuilderReadinessReport } from '../../components/test-builder/builderReadiness'
import { ListeningSectionEditor } from '../../components/test-builder/ListeningSectionEditor'
import {
  RichTextPlainEditor,
} from '../../components/test-builder/RichTextPlainEditor'
import { ReadingSectionEditor } from '../../components/test-builder/ReadingSectionEditor'
import { fillRoute, ROUTES } from '../../constants/routes'
import {
  canEditTeacherTest,
  useTeacherTestBuilderMutations,
  useTeacherTestPreview,
} from '../../hooks/useTeacherTests'
import type {
  ApiFieldError,
  BackendId,
  TeacherTestRDO,
  TeacherTestSectionRDO,
  TestType,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { humanizeToken } from '../../utils/formatters'
import { toPlainRichTextValue } from '../../utils/richText'

type MetadataFormState = {
  title: string
  description: string
  type: TestType
  difficulty: string
  estimatedBandMin: string
  estimatedBandMax: string
  durationMinutes: string
  priceAmount: string
  currency: string
  free: boolean
  language: string
}

type SectionDraft = {
  title: string
  instructions: string
  passageText: string
  audioFileId: string
  durationMinutes: string
}

const testTypes: Array<{ label: string; value: TestType }> = [
  { label: 'Reading', value: 'reading' },
  { label: 'Listening', value: 'listening' },
  { label: 'Writing task 1', value: 'writing_task_1' },
  { label: 'Writing task 2', value: 'writing_task_2' },
  { label: 'Speaking part 1', value: 'speaking_part_1' },
  { label: 'Speaking part 2', value: 'speaking_part_2' },
  { label: 'Speaking part 3', value: 'speaking_part_3' },
  { label: 'Full mock', value: 'full_mock' },
]

const difficulties = ['beginner', 'intermediate', 'advanced']

const metadataFieldNames = [
  'title',
  'description',
  'type',
  'difficulty',
  'estimatedBandMin',
  'estimatedBandMax',
  'durationMinutes',
  'priceAmount',
  'currency',
  'free',
  'language',
] as const

type MetadataFieldName = (typeof metadataFieldNames)[number]

const defaultMetadataForm: MetadataFormState = {
  title: '',
  description: '',
  type: 'reading',
  difficulty: 'intermediate',
  estimatedBandMin: '',
  estimatedBandMax: '',
  durationMinutes: '60',
  priceAmount: '0',
  currency: 'UZS',
  free: true,
  language: 'en',
}

const defaultSectionDraft = (index: number): SectionDraft => ({
  title: `Section ${index + 1}`,
  instructions: '',
  passageText: '',
  audioFileId: '',
  durationMinutes: '',
})

const toStringValue = (value?: number | string | null) =>
  value === undefined || value === null ? '' : String(value)

const metadataFromTest = (test?: TeacherTestRDO | null): MetadataFormState =>
  test
    ? {
        title: test.title ?? '',
        description: test.description ?? '',
        type: (test.type as TestType) || 'reading',
        difficulty: test.difficulty ?? 'intermediate',
        estimatedBandMin: toStringValue(test.estimatedBandMin),
        estimatedBandMax: toStringValue(test.estimatedBandMax),
        durationMinutes: toStringValue(test.durationMinutes),
        priceAmount: toStringValue(test.priceAmount ?? 0),
        currency: test.currency ?? 'UZS',
        free: Boolean(test.free),
        language: test.language ?? 'en',
      }
    : defaultMetadataForm

const sectionDraftFromSection = (section?: TeacherTestSectionRDO | null): SectionDraft =>
  section
    ? {
        title: section.title ?? '',
        instructions:
          section.instructionsContent?.contentText ??
          section.instructions ??
          '',
        passageText:
          section.passageTextContent?.contentText ??
          section.passageText ??
          '',
        audioFileId: toStringValue(section.audioFileId),
        durationMinutes: toStringValue(section.durationMinutes),
      }
    : defaultSectionDraft(0)

const optionalNumber = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? Number(trimmed) : null
}

const optionalString = (value: string) => {
  const trimmed = value.trim()
  return trimmed || null
}

const fieldErrorMap = (fieldErrors: ApiFieldError[]) => {
  const fields: Partial<Record<MetadataFieldName, string>> = {}
  fieldErrors.forEach((error) => {
    if (metadataFieldNames.includes(error.field as MetadataFieldName)) {
      fields[error.field as MetadataFieldName] = error.message
    }
  })
  return fields
}

const orderedSections = (test?: TeacherTestRDO | null) =>
  [...(test?.sections ?? [])].sort((left, right) => left.sectionOrder - right.sectionOrder)

const visibilityTone = (visibility?: string) => {
  if (visibility === 'draft') return 'info'
  if (visibility === 'rejected') return 'danger'
  if (visibility === 'published') return 'success'
  if (visibility === 'archived') return 'neutral'
  return 'warning'
}

function useUnsavedChangesWarning(isDirty: boolean) {
  useEffect(() => {
    if (!isDirty) {
      return undefined
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])
}

function metadataPayload(form: MetadataFormState) {
  return {
    title: optionalString(form.title),
    description: optionalString(form.description),
    type: form.type,
    difficulty: optionalString(form.difficulty),
    estimatedBandMin: optionalNumber(form.estimatedBandMin),
    estimatedBandMax: optionalNumber(form.estimatedBandMax),
    durationMinutes: optionalNumber(form.durationMinutes),
    priceAmount: form.free ? 0 : optionalNumber(form.priceAmount),
    currency: optionalString(form.currency)?.toUpperCase() ?? 'UZS',
    free: form.free,
    language: optionalString(form.language) ?? 'en',
  }
}

function sectionPayload(draft: SectionDraft) {
  const instructions = toPlainRichTextValue(draft.instructions)
  const passage = toPlainRichTextValue(draft.passageText)
  const audioFileId = optionalNumber(draft.audioFileId)

  return {
    title: optionalString(draft.title),
    instructions: optionalString(instructions.text),
    instructionsDocument: instructions.document,
    passageText: optionalString(passage.text),
    passageDocument: passage.document,
    audioFileId,
    durationMinutes: optionalNumber(draft.durationMinutes),
  }
}

function ValidationSummary({ fieldErrors, message }: { fieldErrors: ApiFieldError[]; message?: string }) {
  if (!message && fieldErrors.length === 0) {
    return null
  }

  return (
    <Alert title="Validation summary" variant="danger">
      {message && <p>{message}</p>}
      {fieldErrors.length > 0 && (
        <ul className="test-builder-validation-list">
          {fieldErrors.map((error) => (
            <li key={`${error.field}-${error.code}`}>
              <strong>{error.field}</strong>: {error.message}
            </li>
          ))}
        </ul>
      )}
    </Alert>
  )
}

function MetadataForm({
  disabled,
  errors,
  form,
  isSaving,
  onChange,
  onSubmit,
}: {
  disabled: boolean
  errors: Partial<Record<MetadataFieldName, string>>
  form: MetadataFormState
  isSaving: boolean
  onChange: (next: MetadataFormState) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  const update = <TKey extends keyof MetadataFormState>(
    key: TKey,
    value: MetadataFormState[TKey],
  ) => onChange({ ...form, [key]: value })

  return (
    <form className="test-builder-form" onSubmit={onSubmit}>
      <TextField
        disabled={disabled}
        error={errors.title}
        label="Title"
        onChange={(event) => update('title', event.target.value)}
        required
        value={form.title}
      />
      <TextareaField
        disabled={disabled}
        error={errors.description}
        label="Description"
        onChange={(event) => update('description', event.target.value)}
        value={form.description}
      />
      <div className="test-builder-form-grid">
        <SelectField
          disabled={disabled}
          error={errors.type}
          label="Type"
          onChange={(event) => update('type', event.target.value as TestType)}
          required
          value={form.type}
        >
          {testTypes.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </SelectField>
        <SelectField
          disabled={disabled}
          error={errors.difficulty}
          label="Difficulty"
          onChange={(event) => update('difficulty', event.target.value)}
          value={form.difficulty}
        >
          {difficulties.map((difficulty) => (
            <option key={difficulty} value={difficulty}>
              {humanizeToken(difficulty)}
            </option>
          ))}
        </SelectField>
        <TextField
          disabled={disabled}
          error={errors.estimatedBandMin}
          inputMode="decimal"
          label="Band min"
          onChange={(event) => update('estimatedBandMin', event.target.value)}
          value={form.estimatedBandMin}
        />
        <TextField
          disabled={disabled}
          error={errors.estimatedBandMax}
          inputMode="decimal"
          label="Band max"
          onChange={(event) => update('estimatedBandMax', event.target.value)}
          value={form.estimatedBandMax}
        />
        <TextField
          disabled={disabled}
          error={errors.durationMinutes}
          inputMode="numeric"
          label="Duration minutes"
          onChange={(event) => update('durationMinutes', event.target.value)}
          value={form.durationMinutes}
        />
        <TextField
          disabled={disabled || form.free}
          error={errors.priceAmount}
          inputMode="decimal"
          label="Price amount"
          onChange={(event) => update('priceAmount', event.target.value)}
          value={form.priceAmount}
        />
        <TextField
          disabled={disabled || form.free}
          error={errors.currency}
          label="Currency"
          maxLength={3}
          onChange={(event) => update('currency', event.target.value.toUpperCase())}
          value={form.currency}
        />
        <TextField
          disabled={disabled}
          error={errors.language}
          label="Language"
          onChange={(event) => update('language', event.target.value)}
          value={form.language}
        />
      </div>
      <label className="test-builder-checkbox">
        <input
          checked={form.free}
          disabled={disabled}
          onChange={(event) => update('free', event.target.checked)}
          type="checkbox"
        />
        <span>Free test</span>
      </label>
      <div className="test-builder-actions">
        <Button disabled={disabled} isLoading={isSaving} loadingLabel="Saving" type="submit">
          Save metadata
        </Button>
      </div>
    </form>
  )
}

function SectionList({
  activeSectionId,
  disabled,
  isBusy,
  onAdd,
  onDelete,
  onMove,
  onSelect,
  sections,
}: {
  activeSectionId: BackendId | null
  disabled: boolean
  isBusy: boolean
  onAdd: () => void
  onDelete: (sectionId: BackendId) => void
  onMove: (sectionId: BackendId, direction: -1 | 1) => void
  onSelect: (sectionId: BackendId) => void
  sections: TeacherTestSectionRDO[]
}) {
  return (
    <div className="test-builder-section-list">
      <div className="test-builder-section-list__header">
        <h3>Sections</h3>
        <Button disabled={disabled} isLoading={isBusy} onClick={onAdd} size="sm" variant="secondary">
          Add
        </Button>
      </div>
      {sections.length === 0 ? (
        <EmptyState
          eyebrow="No sections"
          message="Save the draft, then add a section shell for the skill-specific editor."
          title="Start with one section"
        />
      ) : (
        <div className="test-builder-section-list__items">
          {sections.map((section, index) => (
            <RepeatedItemCard
              className={section.id === activeSectionId ? 'test-builder-section-card is-active' : 'test-builder-section-card'}
              key={section.id}
            >
              <button
                className="test-builder-section-card__select"
                onClick={() => onSelect(section.id)}
                type="button"
              >
                <span>Section {section.sectionOrder}</span>
                <strong>{section.title || 'Untitled section'}</strong>
              </button>
              <div className="test-builder-section-card__actions">
                <Button
                  aria-label="Move section up"
                  disabled={disabled || index === 0}
                  onClick={() => onMove(section.id, -1)}
                  size="sm"
                  variant="ghost"
                >
                  Up
                </Button>
                <Button
                  aria-label="Move section down"
                  disabled={disabled || index === sections.length - 1}
                  onClick={() => onMove(section.id, 1)}
                  size="sm"
                  variant="ghost"
                >
                  Down
                </Button>
                <Button
                  disabled={disabled}
                  onClick={() => onDelete(section.id)}
                  size="sm"
                  variant="danger"
                >
                  Delete
                </Button>
              </div>
            </RepeatedItemCard>
          ))}
        </div>
      )}
    </div>
  )
}

function SectionEditor({
  children,
  disabled,
  draft,
  isSaving,
  onChange,
  onSave,
  section,
  showAudioFileId = true,
}: {
  children?: ReactNode
  disabled: boolean
  draft: SectionDraft
  isSaving: boolean
  onChange: (draft: SectionDraft) => void
  onSave: () => void
  section?: TeacherTestSectionRDO
  showAudioFileId?: boolean
}) {
  const update = <TKey extends keyof SectionDraft>(key: TKey, value: SectionDraft[TKey]) =>
    onChange({ ...draft, [key]: value })

  if (!section) {
    return (
      <EmptyState
        eyebrow="Section editor"
        message="Choose or add a section to edit shell metadata. Reading and listening internals plug in below this layer."
        title="No active section"
      />
    )
  }

  return (
    <div className="test-builder-section-editor">
      <div className="test-builder-form-grid">
        <TextField
          disabled={disabled}
          label="Section title"
          onChange={(event) => update('title', event.target.value)}
          value={draft.title}
        />
        <TextField
          disabled={disabled}
          inputMode="numeric"
          label="Section duration"
          onChange={(event) => update('durationMinutes', event.target.value)}
          value={draft.durationMinutes}
        />
        {showAudioFileId && (
          <TextField
            disabled={disabled}
            inputMode="numeric"
            label="Audio file id"
            onChange={(event) => update('audioFileId', event.target.value)}
            value={draft.audioFileId}
          />
        )}
      </div>
      <RichTextPlainEditor
        backendContent={section.instructionsContent}
        disabled={disabled}
        helpText="Saved as backend rich-text JSON; plain text is split into paragraph nodes."
        label="Section instructions"
        onChange={(value) => update('instructions', value.text)}
        value={draft.instructions}
      />
      <RichTextPlainEditor
        backendContent={section.passageTextContent}
        disabled={disabled}
        label="Passage text"
        onChange={(value) => update('passageText', value.text)}
        value={draft.passageText}
      />
      <div className="test-builder-actions">
        <Button disabled={disabled} isLoading={isSaving} loadingLabel="Saving" onClick={onSave}>
          Save section
        </Button>
      </div>
      {children ?? (
        <div className="test-builder-extension-slot">
          <p className="test-builder-muted">Skill editor extension point</p>
          <p>
            Skill-specific editors mount here while reusing the saved test, active section, read-only
            state, rich-text wrapper, and builder mutations.
          </p>
        </div>
      )}
    </div>
  )
}

export function TeacherTestBuilderPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const parsedId = id ? Number(id) : undefined
  const testId = Number.isFinite(parsedId) ? parsedId : undefined
  const isCreateRoute = !id
  const previewQuery = useTeacherTestPreview(testId)
  const mutations = useTeacherTestBuilderMutations(testId)
  const test = previewQuery.data
  const sections = useMemo(() => orderedSections(test), [test])
  const serverForm = useMemo(() => metadataFromTest(test), [test])
  const [formDraft, setFormDraft] = useState<MetadataFormState>(defaultMetadataForm)
  const [metadataDirty, setMetadataDirty] = useState(false)
  const [activeSectionId, setActiveSectionId] = useState<BackendId | null>(null)
  const selectedSectionId = activeSectionId ?? sections[0]?.id ?? null
  const activeSection =
    sections.find((section) => section.id === selectedSectionId) ?? sections[0]
  const serverSectionDraft = useMemo(
    () => sectionDraftFromSection(activeSection),
    [activeSection],
  )
  const [sectionDraftState, setSectionDraftState] = useState<SectionDraft>(defaultSectionDraft(0))
  const [sectionDirty, setSectionDirty] = useState(false)
  const [previewMode, setPreviewMode] = useState<BuilderPreviewMode>('student')
  const [lastError, setLastError] = useState<unknown>(null)
  const [submitSuccessMessage, setSubmitSuccessMessage] = useState<string | null>(null)
  const form = metadataDirty ? formDraft : serverForm
  const sectionDraft = sectionDirty ? sectionDraftState : serverSectionDraft
  const normalizedError = useMemo(
    () => (lastError ? normalizeApiError(lastError) : null),
    [lastError],
  )
  const normalizedFieldErrors = useMemo(
    () => normalizedError?.fieldErrors ?? [],
    [normalizedError],
  )
  const fieldErrors = fieldErrorMap(normalizedError?.fieldErrors ?? [])
  const readOnly = Boolean(test && !canEditTeacherTest(test))
  const canEdit = !readOnly
  const hasUnsavedChanges = metadataDirty || sectionDirty
  const readinessReport = useMemo(
    () =>
      buildBuilderReadinessReport({
        fieldErrors: normalizedFieldErrors,
        hasUnsavedChanges,
        metadata: form,
        test,
      }),
    [form, hasUnsavedChanges, normalizedFieldErrors, test],
  )
  const isSavingMetadata = mutations.createTest.isPending || mutations.updateTest.isPending
  const isSectionBusy =
    mutations.addSection.isPending ||
    mutations.updateSection.isPending ||
    mutations.reorderSections.isPending ||
    mutations.deleteSection.isPending
  const isParagraphBusy =
    mutations.addParagraph.isPending ||
    mutations.updateParagraph.isPending ||
    mutations.reorderParagraphs.isPending ||
    mutations.deleteParagraph.isPending
  const isSegmentBusy =
    mutations.addListeningSegment.isPending ||
    mutations.updateListeningSegment.isPending ||
    mutations.reorderListeningSegments.isPending ||
    mutations.deleteListeningSegment.isPending
  const isQuestionBusy =
    mutations.addQuestion.isPending ||
    mutations.updateQuestion.isPending ||
    mutations.deleteQuestion.isPending
  const submitDisabled =
    !test ||
    !canEdit ||
    mutations.submitTest.isPending ||
    !readinessReport.readyToSubmit

  useUnsavedChangesWarning(metadataDirty || sectionDirty)

  const updateMetadata = (next: MetadataFormState) => {
    setFormDraft(next)
    setMetadataDirty(true)
    setSubmitSuccessMessage(null)
  }

  const updateSectionDraft = (next: SectionDraft) => {
    setSectionDraftState(next)
    setSectionDirty(true)
    setSubmitSuccessMessage(null)
  }

  const saveMetadata = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const payload = metadataPayload(form)

    if (isCreateRoute) {
      try {
        const created = await mutations.createTest.mutateAsync(payload)
        setLastError(null)
        setSubmitSuccessMessage(null)
        setMetadataDirty(false)
        navigate(fillRoute(ROUTES.teacherTestEdit, { id: created.id }), { replace: true })
      } catch (error) {
        setLastError(error)
      }
      return
    }

    try {
      await mutations.updateTest.mutateAsync(payload)
      setLastError(null)
      setSubmitSuccessMessage(null)
      setMetadataDirty(false)
    } catch (error) {
      setLastError(error)
    }
  }

  const addSection = async () => {
    try {
      const created = await mutations.addSection.mutateAsync(
        sectionPayload(defaultSectionDraft(sections.length)),
      )
      setLastError(null)
      setSubmitSuccessMessage(null)
      const newSection = orderedSections(created).at(-1)
      if (newSection) {
        setActiveSectionId(newSection.id)
      }
    } catch (error) {
      setLastError(error)
    }
  }

  const saveSection = async () => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.updateSection.mutateAsync({
        sectionId: activeSection.id,
        payload: sectionPayload(sectionDraft),
      })
      setLastError(null)
      setSubmitSuccessMessage(null)
      setSectionDirty(false)
    } catch (error) {
      setLastError(error)
    }
  }

  const moveSection = async (sectionId: BackendId, direction: -1 | 1) => {
    const currentIndex = sections.findIndex((section) => section.id === sectionId)
    const targetIndex = currentIndex + direction
    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= sections.length) {
      return
    }

    const nextIds = sections.map((section) => section.id)
    const [moved] = nextIds.splice(currentIndex, 1)
    nextIds.splice(targetIndex, 0, moved)
    try {
      await mutations.reorderSections.mutateAsync({ sectionIds: nextIds })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
    }
  }

  const deleteSection = async (sectionId: BackendId) => {
    const confirmed = window.confirm('Delete this section? This also removes its builder content.')
    if (!confirmed) {
      return
    }

    try {
      await mutations.deleteSection.mutateAsync(sectionId)
      setLastError(null)
      setSubmitSuccessMessage(null)
      setActiveSectionId(null)
      setSectionDirty(false)
    } catch (error) {
      setLastError(error)
    }
  }

  const addParagraph = async (payload: Parameters<typeof mutations.addParagraph.mutateAsync>[0]['payload']) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.addParagraph.mutateAsync({ payload, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const updateParagraph = async (
    paragraphId: BackendId,
    payload: Parameters<typeof mutations.updateParagraph.mutateAsync>[0]['payload'],
  ) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.updateParagraph.mutateAsync({ paragraphId, payload, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const reorderParagraphs = async (paragraphIds: BackendId[]) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.reorderParagraphs.mutateAsync({
        payload: { paragraphIds },
        sectionId: activeSection.id,
      })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const deleteParagraph = async (paragraphId: BackendId) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.deleteParagraph.mutateAsync({ paragraphId, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const addListeningSegment = async (
    payload: Parameters<typeof mutations.addListeningSegment.mutateAsync>[0]['payload'],
  ) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.addListeningSegment.mutateAsync({ payload, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const updateListeningSegment = async (
    segmentId: BackendId,
    payload: Parameters<typeof mutations.updateListeningSegment.mutateAsync>[0]['payload'],
  ) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.updateListeningSegment.mutateAsync({ payload, sectionId: activeSection.id, segmentId })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const reorderListeningSegments = async (segmentIds: BackendId[]) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.reorderListeningSegments.mutateAsync({
        payload: { segmentIds },
        sectionId: activeSection.id,
      })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const deleteListeningSegment = async (segmentId: BackendId) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.deleteListeningSegment.mutateAsync({ sectionId: activeSection.id, segmentId })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const addQuestion = async (payload: Parameters<typeof mutations.addQuestion.mutateAsync>[0]['payload']) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.addQuestion.mutateAsync({ payload, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const updateQuestion = async (
    questionId: BackendId,
    payload: Parameters<typeof mutations.updateQuestion.mutateAsync>[0]['payload'],
  ) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.updateQuestion.mutateAsync({ payload, questionId, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const deleteQuestion = async (questionId: BackendId) => {
    if (!activeSection) {
      return
    }

    try {
      await mutations.deleteQuestion.mutateAsync({ questionId, sectionId: activeSection.id })
      setLastError(null)
      setSubmitSuccessMessage(null)
    } catch (error) {
      setLastError(error)
      throw error
    }
  }

  const submitForModeration = async () => {
    if (!test || submitDisabled) {
      return
    }

    const confirmed = window.confirm(
      'Submit this saved draft for moderation? It will become read-only until an admin reviews it.',
    )
    if (!confirmed) {
      return
    }

    try {
      await mutations.submitTest.mutateAsync()
      setLastError(null)
      setSubmitSuccessMessage('Submitted for moderation. The test is now read-only for teachers.')
      setPreviewMode('readiness')
    } catch (error) {
      setLastError(error)
      setSubmitSuccessMessage(null)
      setPreviewMode('readiness')
    }
  }

  if (id && !testId) {
    return (
      <PageContainer>
        <PageHeader eyebrow="Builder" title="Unable to open test" />
        <Alert title="Invalid test id" variant="danger">
          The edit route needs a numeric teacher test id.
        </Alert>
      </PageContainer>
    )
  }

  if (previewQuery.isLoading && !isCreateRoute) {
    return (
      <PageContainer size="wide">
        <PageHeader eyebrow="Builder" title="Loading test builder" />
        <Panel>
          <div className="ui-panel-state">
            <Loader label="Loading builder preview" />
            <Skeleton lines={6} />
          </div>
        </Panel>
      </PageContainer>
    )
  }

  if (previewQuery.isError && !isCreateRoute) {
    const error = normalizeApiError(previewQuery.error)
    return (
      <PageContainer>
        <PageHeader eyebrow="Builder" title="Unable to open test" />
        <Alert title={error.code} variant="danger">
          {error.message}
        </Alert>
      </PageContainer>
    )
  }

  return (
    <PageContainer className="test-builder-page" size="wide">
      <PageHeader
        actions={
          <div className="test-builder-header-actions">
            {test && <Badge tone={visibilityTone(test.visibility)}>{humanizeToken(test.visibility)}</Badge>}
            {(metadataDirty || sectionDirty) && <Badge tone="warning">Unsaved changes</Badge>}
          </div>
        }
        eyebrow="Builder"
        title={isCreateRoute ? 'Create test' : form.title || 'Edit test'}
      />

      {readOnly && (
        <Alert title="Read-only test" variant="warning">
          This test is {humanizeToken(String(test?.visibility))}. Metadata and section commands are
          disabled, but the preview remains available.
        </Alert>
      )}
      {test?.visibility === 'rejected' && (
        <Alert title="Moderation note" variant="danger">
          {test.moderationNote || 'The moderation team requested changes before this test can be published.'}
          <br />
          Edit the saved draft and resubmit it when the readiness checklist is clear.
        </Alert>
      )}
      {submitSuccessMessage && (
        <Alert title="Submitted" variant="success">
          {submitSuccessMessage}
        </Alert>
      )}
      <ValidationSummary
        fieldErrors={normalizedFieldErrors}
        message={normalizedError?.message}
      />

      <div className="test-builder-grid">
        <div className="test-builder-main">
          <Panel
            description="Client readiness is guidance. The backend submit check remains the final authority."
            title="Submit for moderation"
          >
            <div className="test-builder-submit-panel">
              <div>
                <p className="test-builder-muted">Readiness</p>
                <h3>{readinessReport.readyToSubmit ? 'Ready for backend submit check' : 'Not ready yet'}</h3>
                <p>
                  {readinessReport.readyToSubmit
                    ? 'Submit sends the saved server draft for moderation and locks teacher edits until review.'
                    : 'Resolve blocking checklist items and save local edits before submitting.'}
                </p>
              </div>
              <div className="test-builder-submit-panel__actions">
                <Badge tone={readinessReport.readyToSubmit ? 'success' : 'danger'}>
                  {readinessReport.readyToSubmit ? 'Ready' : `${readinessReport.blockedCount} blocking`}
                </Badge>
                <Button
                  disabled={submitDisabled}
                  isLoading={mutations.submitTest.isPending}
                  loadingLabel="Submitting"
                  onClick={() => void submitForModeration()}
                >
                  Submit
                </Button>
              </div>
            </div>
          </Panel>

          <Panel description="Drafts use an explicit save pattern. The server remains authoritative after each save." title="Metadata">
            <MetadataForm
              disabled={!canEdit}
              errors={fieldErrors}
              form={form}
              isSaving={isSavingMetadata}
              onChange={updateMetadata}
              onSubmit={saveMetadata}
            />
          </Panel>

          <Panel description="Section shells are shared across IELTS skills. Specialized editors can mount into the active section." title="Builder sections">
            {!test && isCreateRoute ? (
              <EmptyState
                eyebrow="Draft required"
                message="Save metadata once to create the draft, then add and reorder sections."
                title="Create the draft first"
              />
            ) : (
              <div className="test-builder-sections-layout">
                <SectionList
                  activeSectionId={activeSection?.id ?? null}
                  disabled={!canEdit}
                  isBusy={isSectionBusy}
                  onAdd={addSection}
                  onDelete={deleteSection}
                  onMove={moveSection}
                  onSelect={(sectionId) => {
                    setActiveSectionId(sectionId)
                    setSectionDirty(false)
                  }}
                  sections={sections}
                />
                <SectionEditor
                  disabled={!canEdit}
                  draft={sectionDraft}
                  isSaving={mutations.updateSection.isPending}
                  onChange={updateSectionDraft}
                  onSave={saveSection}
                  section={activeSection}
                  showAudioFileId={form.type !== 'listening'}
                >
                  {form.type === 'reading' && activeSection ? (
                    <ReadingSectionEditor
                      disabled={!canEdit}
                      fieldErrors={normalizedError?.fieldErrors ?? []}
                      isParagraphBusy={isParagraphBusy}
                      isQuestionBusy={isQuestionBusy}
                      onAddParagraph={addParagraph}
                      onAddQuestion={addQuestion}
                      onDeleteParagraph={deleteParagraph}
                      onDeleteQuestion={deleteQuestion}
                      onReorderParagraphs={reorderParagraphs}
                      onUpdateParagraph={updateParagraph}
                      onUpdateQuestion={updateQuestion}
                      section={activeSection}
                    />
                  ) : form.type === 'listening' && activeSection ? (
                    <ListeningSectionEditor
                      audioFileId={sectionDraft.audioFileId}
                      disabled={!canEdit}
                      fieldErrors={normalizedError?.fieldErrors ?? []}
                      isQuestionBusy={isQuestionBusy}
                      isSectionSaving={mutations.updateSection.isPending}
                      isSegmentBusy={isSegmentBusy}
                      onAddQuestion={addQuestion}
                      onAddSegment={addListeningSegment}
                      onAudioFileIdChange={(value) =>
                        updateSectionDraft({ ...sectionDraft, audioFileId: value })
                      }
                      onDeleteQuestion={deleteQuestion}
                      onDeleteSegment={deleteListeningSegment}
                      onReorderSegments={reorderListeningSegments}
                      onSaveAudioFileId={saveSection}
                      onUpdateQuestion={updateQuestion}
                      onUpdateSegment={updateListeningSegment}
                      section={activeSection}
                    />
                  ) : null}
                </SectionEditor>
              </div>
            )}
          </Panel>
        </div>

        <BuilderPreviewPanel
          activeSectionId={activeSection?.id}
          fieldErrors={normalizedFieldErrors}
          hasMetadataDraft={metadataDirty}
          metadata={form}
          mode={previewMode}
          onModeChange={setPreviewMode}
          readinessReport={readinessReport}
          sectionDirty={sectionDirty}
          sectionDraft={sectionDraft}
          test={test}
        />
      </div>
    </PageContainer>
  )
}
