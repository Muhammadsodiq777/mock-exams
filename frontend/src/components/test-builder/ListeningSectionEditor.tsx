import { useMemo, useState, type ChangeEvent } from 'react'

import { Alert, Badge, Button, EmptyState, RepeatedItemCard, TextareaField, TextField } from '../ui'
import { useFileDownloadUrl } from '../../hooks/useFiles'
import { backendService } from '../../services/backend.service'
import type {
  ApiFieldError,
  BackendId,
  FileRDO,
  TeacherListeningSegmentCDO,
  TeacherListeningSegmentRDO,
  TeacherListeningSegmentUDO,
  TeacherQuestionCDO,
  TeacherQuestionRDO,
  TeacherTestSectionRDO,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { QuestionEditor } from './QuestionEditor'

type SegmentDraft = {
  endSeconds: string
  startSeconds: string
  title: string
  transcript: string
}

type UploadState = {
  error?: string
  file?: FileRDO
  progress?: number
  status: 'idle' | 'uploading' | 'success' | 'error'
}

type ListeningSectionEditorProps = {
  audioFileId: string
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isQuestionBusy: boolean
  isSectionSaving: boolean
  isSegmentBusy: boolean
  onAddQuestion: (payload: TeacherQuestionCDO) => Promise<void>
  onAddSegment: (payload: TeacherListeningSegmentCDO) => Promise<void>
  onAudioFileIdChange: (value: string) => void
  onDeleteQuestion: (questionId: BackendId) => Promise<void>
  onDeleteSegment: (segmentId: BackendId) => Promise<void>
  onReorderSegments: (segmentIds: BackendId[]) => Promise<void>
  onSaveAudioFileId: () => Promise<void>
  onUpdateQuestion: (questionId: BackendId, payload: TeacherQuestionCDO) => Promise<void>
  onUpdateSegment: (segmentId: BackendId, payload: TeacherListeningSegmentUDO) => Promise<void>
  section: TeacherTestSectionRDO
}

const optionalString = (value: string) => {
  const trimmed = value.trim()
  return trimmed || null
}

const orderedSegments = (section: TeacherTestSectionRDO) =>
  [...(section.listeningSegments ?? [])].sort((left, right) => left.segmentOrder - right.segmentOrder)

const orderedQuestions = (section: TeacherTestSectionRDO) =>
  [...(section.questions ?? [])].sort((left, right) => left.questionOrder - right.questionOrder)

const segmentDraftFromSegment = (segment?: TeacherListeningSegmentRDO): SegmentDraft => ({
  endSeconds: segment?.endSeconds === undefined || segment.endSeconds === null ? '' : String(segment.endSeconds),
  startSeconds: segment?.startSeconds === undefined || segment.startSeconds === null ? '' : String(segment.startSeconds),
  title: segment?.title ?? '',
  transcript: segment?.transcript ?? '',
})

const parseWholeSeconds = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const parsed = Number(trimmed)
  return Number.isFinite(parsed) && Number.isInteger(parsed) ? parsed : null
}

const segmentPayload = (draft: SegmentDraft): TeacherListeningSegmentCDO | null => {
  const startSeconds = parseWholeSeconds(draft.startSeconds)
  const endSeconds = parseWholeSeconds(draft.endSeconds)

  if (startSeconds === null || endSeconds === null || startSeconds < 0 || endSeconds < 0 || endSeconds <= startSeconds) {
    return null
  }

  return {
    endSeconds,
    startSeconds,
    title: optionalString(draft.title),
    transcript: optionalString(draft.transcript),
  }
}

const segmentValidationMessage = (draft: SegmentDraft) => {
  const startSeconds = parseWholeSeconds(draft.startSeconds)
  const endSeconds = parseWholeSeconds(draft.endSeconds)

  if (startSeconds === null || endSeconds === null) {
    return 'Use whole-second numeric offsets for start and end.'
  }

  if (startSeconds < 0 || endSeconds < 0) {
    return 'Segment times cannot be negative.'
  }

  if (endSeconds <= startSeconds) {
    return 'End time must be greater than start time.'
  }

  return null
}

const fieldError = (fieldErrors: ApiFieldError[], candidates: string[]) =>
  fieldErrors.find((error) =>
    candidates.some((candidate) => error.field === candidate || error.field.endsWith(`.${candidate}`)),
  )?.message

const maybeBackendId = (value: string) => {
  const parsed = Number(value.trim())
  return Number.isFinite(parsed) && Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

const formatSeconds = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--:--'
  }

  const minutes = Math.floor(value / 60)
  const seconds = value % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

const segmentLabel = (segment: TeacherListeningSegmentRDO) =>
  `${segment.segmentOrder}. ${segment.title || 'Untitled segment'} (${formatSeconds(segment.startSeconds)}-${formatSeconds(segment.endSeconds)})`

function AudioMetadataPanel({
  audioFileId,
  disabled,
  isSaving,
  onAudioFileIdChange,
  onSaveAudioFileId,
  savedAudioFileId,
}: {
  audioFileId: string
  disabled: boolean
  isSaving: boolean
  onAudioFileIdChange: (value: string) => void
  onSaveAudioFileId: () => Promise<void>
  savedAudioFileId?: BackendId | null
}) {
  const [durationSeconds, setDurationSeconds] = useState<number | null>(null)
  const [lastUploadFile, setLastUploadFile] = useState<File | null>(null)
  const [uploadState, setUploadState] = useState<UploadState>({ status: 'idle' })
  const previewFileId = maybeBackendId(audioFileId)
  const downloadUrlQuery = useFileDownloadUrl(previewFileId)

  const uploadFile = async (file: File) => {
    const payload = new FormData()
    payload.append('file', file)
    payload.append('purpose', 'listening_audio')

    setLastUploadFile(file)
    setUploadState({ progress: 0, status: 'uploading' })

    try {
      const uploaded = await backendService.files.uploadTeacherFile(payload, {
        onUploadProgress: (event) => {
          const total = event.total
          if (!total) {
            return
          }

          setUploadState((current) => ({
            ...current,
            progress: Math.round((event.loaded / total) * 100),
          }))
        },
      })
      onAudioFileIdChange(String(uploaded.id))
      setDurationSeconds(null)
      setUploadState({ file: uploaded, progress: 100, status: 'success' })
    } catch (error) {
      const normalized = normalizeApiError(error)
      setUploadState({
        error: normalized.message,
        progress: undefined,
        status: 'error',
      })
    }
  }

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    void uploadFile(file)
    event.target.value = ''
  }

  return (
    <RepeatedItemCard className="test-builder-audio-panel">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">Audio source</p>
          <h4>Listening audio</h4>
        </div>
        <Badge tone={previewFileId ? 'success' : 'warning'}>
          {previewFileId ? `Draft file ${previewFileId}` : 'No audio selected'}
        </Badge>
      </div>

      <div className="test-builder-audio-metadata">
        <span>Saved file id: {savedAudioFileId ?? 'none'}</span>
        <span>Draft file id: {audioFileId.trim() || 'none'}</span>
        <span>Duration: {durationSeconds === null ? 'unknown' : formatSeconds(durationSeconds)}</span>
        {uploadState.file && <span>Uploaded: {uploadState.file.filename}</span>}
      </div>

      <div className="test-builder-upload-row">
        <label className="test-builder-file-button">
          <input
            accept="audio/*"
            disabled={disabled || uploadState.status === 'uploading'}
            onChange={handleFileChange}
            type="file"
          />
          <span>Choose audio</span>
        </label>
        <Button
          disabled={disabled || !lastUploadFile || uploadState.status === 'uploading'}
          onClick={() => lastUploadFile && void uploadFile(lastUploadFile)}
          size="sm"
          variant="secondary"
        >
          Retry upload
        </Button>
      </div>

      {uploadState.status === 'uploading' && (
        <div className="test-builder-upload-progress">
          <progress max={100} value={uploadState.progress ?? undefined} />
          <span>{uploadState.progress === undefined ? 'Uploading' : `${uploadState.progress}%`}</span>
        </div>
      )}
      {uploadState.status === 'success' && (
        <Alert title="Upload complete" variant="success">
          The uploaded file id is in the section draft. Save the section to persist it on this Listening part.
        </Alert>
      )}
      {uploadState.status === 'error' && (
        <Alert title="Upload failed" variant="danger">
          {uploadState.error ?? 'The audio upload failed. Your unsaved segments and questions were left untouched.'}
        </Alert>
      )}

      {downloadUrlQuery.isError && (
        <Alert title="Audio preview unavailable" variant="warning">
          The signed audio URL could not be loaded. Save and retry, or upload the audio again.
        </Alert>
      )}
      {downloadUrlQuery.data?.url && (
        <audio
          aria-label="Listening audio preview"
          controls
          onLoadedMetadata={(event) => {
            const nextDuration = Math.round(event.currentTarget.duration)
            setDurationSeconds(Number.isFinite(nextDuration) ? nextDuration : null)
          }}
          src={downloadUrlQuery.data.url}
        />
      )}

      <div className="test-builder-actions">
        <Button
          disabled={disabled || !audioFileId.trim()}
          onClick={() => onAudioFileIdChange('')}
          size="sm"
          variant="ghost"
        >
          Clear audio
        </Button>
        <Button
          disabled={disabled}
          isLoading={isSaving}
          loadingLabel="Saving"
          onClick={() => void onSaveAudioFileId().catch(() => undefined)}
          size="sm"
        >
          Save audio file id
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

function SegmentEditor({
  disabled,
  fieldErrors,
  isBusy,
  onDelete,
  onMove,
  onSave,
  position,
  segment,
  total,
}: {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isBusy: boolean
  onDelete: (segmentId: BackendId) => Promise<void>
  onMove: (segmentId: BackendId, direction: -1 | 1) => Promise<void>
  onSave: (segmentId: BackendId, payload: TeacherListeningSegmentUDO) => Promise<void>
  position: number
  segment: TeacherListeningSegmentRDO
  total: number
}) {
  const [clientError, setClientError] = useState<string | null>(null)
  const [draft, setDraft] = useState<SegmentDraft>(() => segmentDraftFromSegment(segment))

  const update = <TKey extends keyof SegmentDraft>(key: TKey, value: SegmentDraft[TKey]) => {
    setClientError(null)
    setDraft((current) => ({ ...current, [key]: value }))
  }

  const save = async () => {
    const payload = segmentPayload(draft)
    if (!payload) {
      setClientError(segmentValidationMessage(draft))
      return
    }

    await onSave(segment.id, payload)
  }

  return (
    <RepeatedItemCard className="test-builder-segment-editor">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">Segment {segment.segmentOrder}</p>
          <h4>{draft.title || 'Untitled segment'}</h4>
        </div>
        <div className="test-builder-actions">
          <Button
            aria-label="Move segment up"
            disabled={disabled || position === 0}
            onClick={() => void onMove(segment.id, -1).catch(() => undefined)}
            size="sm"
            variant="ghost"
          >
            Up
          </Button>
          <Button
            aria-label="Move segment down"
            disabled={disabled || position === total - 1}
            onClick={() => void onMove(segment.id, 1).catch(() => undefined)}
            size="sm"
            variant="ghost"
          >
            Down
          </Button>
          <Button
            disabled={disabled}
            onClick={() => void onDelete(segment.id).catch(() => undefined)}
            size="sm"
            variant="danger"
          >
            Delete
          </Button>
        </div>
      </div>

      {clientError && (
        <Alert title="Segment timing" variant="danger">
          {clientError}
        </Alert>
      )}
      <div className="test-builder-form-grid">
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['title'])}
          label="Segment title"
          onChange={(event) => update('title', event.target.value)}
          value={draft.title}
        />
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['startSeconds'])}
          inputMode="numeric"
          label="Start seconds"
          onChange={(event) => update('startSeconds', event.target.value)}
          value={draft.startSeconds}
        />
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['endSeconds'])}
          inputMode="numeric"
          label="End seconds"
          onChange={(event) => update('endSeconds', event.target.value)}
          value={draft.endSeconds}
        />
      </div>
      <TextareaField
        disabled={disabled}
        error={fieldError(fieldErrors, ['transcript'])}
        label="Transcript"
        onChange={(event) => update('transcript', event.target.value)}
        value={draft.transcript}
      />
      <div className="test-builder-actions">
        <Button
          disabled={disabled}
          isLoading={isBusy}
          loadingLabel="Saving"
          onClick={() => void save().catch(() => undefined)}
        >
          Save segment
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

function NewSegmentEditor({
  disabled,
  fieldErrors,
  isBusy,
  onAdd,
}: {
  disabled: boolean
  fieldErrors: ApiFieldError[]
  isBusy: boolean
  onAdd: (payload: TeacherListeningSegmentCDO) => Promise<void>
}) {
  const [clientError, setClientError] = useState<string | null>(null)
  const [draft, setDraft] = useState<SegmentDraft>(() => ({
    endSeconds: '',
    startSeconds: '',
    title: '',
    transcript: '',
  }))

  const update = <TKey extends keyof SegmentDraft>(key: TKey, value: SegmentDraft[TKey]) => {
    setClientError(null)
    setDraft((current) => ({ ...current, [key]: value }))
  }

  const add = async () => {
    const payload = segmentPayload(draft)
    if (!payload) {
      setClientError(segmentValidationMessage(draft))
      return
    }

    await onAdd(payload)
    setDraft({ endSeconds: '', startSeconds: '', title: '', transcript: '' })
  }

  return (
    <RepeatedItemCard className="test-builder-segment-editor">
      <div className="test-builder-editor-heading">
        <div>
          <p className="test-builder-muted">New segment</p>
          <h4>Add audio segment</h4>
        </div>
      </div>
      {clientError && (
        <Alert title="Segment timing" variant="danger">
          {clientError}
        </Alert>
      )}
      <div className="test-builder-form-grid">
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['title'])}
          label="Segment title"
          onChange={(event) => update('title', event.target.value)}
          value={draft.title}
        />
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['startSeconds'])}
          inputMode="numeric"
          label="Start seconds"
          onChange={(event) => update('startSeconds', event.target.value)}
          value={draft.startSeconds}
        />
        <TextField
          disabled={disabled}
          error={fieldError(fieldErrors, ['endSeconds'])}
          inputMode="numeric"
          label="End seconds"
          onChange={(event) => update('endSeconds', event.target.value)}
          value={draft.endSeconds}
        />
      </div>
      <TextareaField
        disabled={disabled}
        error={fieldError(fieldErrors, ['transcript'])}
        label="Transcript"
        onChange={(event) => update('transcript', event.target.value)}
        value={draft.transcript}
      />
      <div className="test-builder-actions">
        <Button
          disabled={disabled}
          isLoading={isBusy}
          loadingLabel="Adding"
          onClick={() => void add().catch(() => undefined)}
        >
          Add segment
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

export function ListeningSectionEditor({
  audioFileId,
  disabled,
  fieldErrors,
  isQuestionBusy,
  isSectionSaving,
  isSegmentBusy,
  onAddQuestion,
  onAddSegment,
  onAudioFileIdChange,
  onDeleteQuestion,
  onDeleteSegment,
  onReorderSegments,
  onSaveAudioFileId,
  onUpdateQuestion,
  onUpdateSegment,
  section,
}: ListeningSectionEditorProps) {
  const segments = useMemo(() => orderedSegments(section), [section])
  const questions = useMemo(() => orderedQuestions(section), [section])
  const segmentOptions = useMemo(
    () => segments.map((segment) => ({ id: segment.id, label: segmentLabel(segment) })),
    [segments],
  )

  const moveSegment = async (segmentId: BackendId, direction: -1 | 1) => {
    const currentIndex = segments.findIndex((segment) => segment.id === segmentId)
    const targetIndex = currentIndex + direction
    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= segments.length) {
      return
    }

    const nextIds = segments.map((segment) => segment.id)
    const [moved] = nextIds.splice(currentIndex, 1)
    nextIds.splice(targetIndex, 0, moved)
    await onReorderSegments(nextIds)
  }

  return (
    <div className="test-builder-listening-editor">
      <Alert title="Listening content" variant="info">
        Upload audio with the listening_audio purpose, save the section, then link questions only to
        segments saved in this active section.
      </Alert>

      <section className="test-builder-listening-editor__section">
        <AudioMetadataPanel
          audioFileId={audioFileId}
          disabled={disabled}
          isSaving={isSectionSaving}
          onAudioFileIdChange={onAudioFileIdChange}
          onSaveAudioFileId={onSaveAudioFileId}
          savedAudioFileId={section.audioFileId}
        />
      </section>

      <section className="test-builder-listening-editor__section">
        <div className="test-builder-editor-heading">
          <div>
            <p className="test-builder-muted">Audio outline</p>
            <h3>Segments</h3>
          </div>
          <Badge tone={segments.length ? 'success' : 'warning'}>
            {segments.length} segment{segments.length === 1 ? '' : 's'}
          </Badge>
        </div>
        {segments.length === 0 ? (
          <EmptyState
            eyebrow="No segments"
            message="Add timeboxed audio segments with optional teacher transcripts, then link questions to those saved rows."
            title="Build the listening outline"
          />
        ) : (
          segments.map((segment, index) => (
            <SegmentEditor
              disabled={disabled}
              fieldErrors={fieldErrors}
              isBusy={isSegmentBusy}
              key={segment.id}
              onDelete={async (segmentId) => {
                const confirmed = window.confirm('Delete this segment? Related question links may need updates.')
                if (confirmed) {
                  await onDeleteSegment(segmentId)
                }
              }}
              onMove={moveSegment}
              onSave={onUpdateSegment}
              position={index}
              segment={segment}
              total={segments.length}
            />
          ))
        )}
        <NewSegmentEditor
          disabled={disabled}
          fieldErrors={fieldErrors}
          isBusy={isSegmentBusy}
          onAdd={onAddSegment}
        />
      </section>

      <section className="test-builder-listening-editor__section">
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
            listeningSegments={segmentOptions}
            onDelete={async (questionId) => {
              const confirmed = window.confirm('Delete this question?')
              if (confirmed) {
                await onDeleteQuestion(questionId)
              }
            }}
            onSave={(payload) => onUpdateQuestion(question.id, payload)}
            paragraphLabels={[]}
            question={question}
          />
        ))}
        <QuestionEditor
          disabled={disabled}
          fieldErrors={fieldErrors}
          isBusy={isQuestionBusy}
          listeningSegments={segmentOptions}
          onSave={onAddQuestion}
          paragraphLabels={[]}
        />
      </section>
    </div>
  )
}
