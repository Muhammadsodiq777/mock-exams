import {
  Alert,
  Badge,
  Button,
  DescriptionList,
  EmptyState,
  Panel,
  RichContent,
} from '../ui'
import type {
  ApiFieldError,
  BackendId,
  TeacherQuestionRDO,
  TeacherTestRDO,
  TeacherTestSectionRDO,
} from '../../types/backend'
import { humanizeToken } from '../../utils/formatters'
import { richTextPreviewHtml } from '../../utils/richText'
import type {
  BuilderPreviewMetadata,
  BuilderReadinessReport,
  BuilderReadinessStatus,
} from './builderReadiness'

export type BuilderPreviewMode = 'student' | 'author' | 'readiness'

export type BuilderPreviewSectionDraft = {
  instructions: string
  passageText: string
}

const orderedSections = (test?: TeacherTestRDO | null) =>
  [...(test?.sections ?? [])].sort((left, right) => left.sectionOrder - right.sectionOrder)

const orderedQuestions = (section: TeacherTestSectionRDO) =>
  [...(section.questions ?? [])].sort((left, right) => left.questionOrder - right.questionOrder)

const orderedParagraphs = (section: TeacherTestSectionRDO) =>
  [...(section.paragraphs ?? [])].sort((left, right) => left.paragraphOrder - right.paragraphOrder)

const orderedListeningSegments = (section: TeacherTestSectionRDO) =>
  [...(section.listeningSegments ?? [])].sort((left, right) => left.segmentOrder - right.segmentOrder)

const statusTone = (status: BuilderReadinessStatus) => {
  if (status === 'ready') return 'success'
  if (status === 'warning') return 'warning'
  return 'danger'
}

function ReadinessSummary({
  fieldErrors,
  report,
}: {
  fieldErrors: ApiFieldError[]
  report: BuilderReadinessReport
}) {
  return (
    <div className="test-builder-readiness">
      <div className="test-builder-readiness__summary">
        <Badge tone={report.readyToSubmit ? 'success' : 'danger'}>
          {report.readyToSubmit ? 'Ready to submit' : `${report.blockedCount} blocking`}
        </Badge>
        {report.warningCount > 0 && <Badge tone="warning">{report.warningCount} warning</Badge>}
      </div>
      <ul className="test-builder-readiness-list">
        {report.items.map((item) => (
          <li key={item.id} className={`test-builder-readiness-item is-${item.status}`}>
            <Badge tone={statusTone(item.status)}>{humanizeToken(item.status)}</Badge>
            <div>
              <strong>{item.label}</strong>
              <p>{item.detail}</p>
            </div>
          </li>
        ))}
      </ul>
      {fieldErrors.length > 0 && (
        <div className="test-builder-readiness__field-errors">
          <p className="test-builder-muted">Server field errors</p>
          <ul className="test-builder-validation-list">
            {fieldErrors.map((error) => (
              <li key={`${error.field}-${error.code}-${error.message}`}>
                <strong>{error.field}</strong>: {error.message}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function QuestionPreview({
  mode,
  question,
}: {
  mode: Exclude<BuilderPreviewMode, 'readiness'>
  question: TeacherQuestionRDO
}) {
  const showTeacherOnly = mode === 'author'

  return (
    <li className="test-builder-preview-question">
      <div>
        <span>Q{question.questionOrder}</span>
        <strong>{humanizeToken(question.questionType)}</strong>
        {showTeacherOnly && question.score !== undefined && question.score !== null && (
          <Badge tone="neutral">{question.score} pt</Badge>
        )}
      </div>
      {question.questionTextContent?.renderedHtml ? (
        <RichContent trustedHtml={question.questionTextContent.renderedHtml} />
      ) : (
        <p>{question.questionText}</p>
      )}
      {question.instructionsContent?.renderedHtml && (
        <div className="test-builder-preview-note">
          <p className="test-builder-muted">Instructions</p>
          <RichContent trustedHtml={question.instructionsContent.renderedHtml} />
        </div>
      )}
      {(question.options ?? []).length > 0 && (
        <ul className="test-builder-preview-options">
          {(question.options ?? []).map((option) => (
            <li key={option.id}>
              <span>{option.optionLabel}</span>
              {option.optionTextContent?.renderedHtml ? (
                <RichContent trustedHtml={option.optionTextContent.renderedHtml} />
              ) : (
                <p>{option.optionText}</p>
              )}
            </li>
          ))}
        </ul>
      )}
      {showTeacherOnly && (question.answerKeys ?? []).length > 0 && (
        <div className="test-builder-answer-keys">
          <p className="test-builder-muted">Answer keys</p>
          <ul>
            {(question.answerKeys ?? []).map((answer) => (
              <li key={answer.id}>
                {answer.answerValue}
                {answer.primary ? ' (primary)' : ''}
                {answer.explanationContent?.renderedHtml ? (
                  <RichContent trustedHtml={answer.explanationContent.renderedHtml} />
                ) : answer.explanation ? (
                  <p>{answer.explanation}</p>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      )}
    </li>
  )
}

function SectionPreview({
  activeSectionId,
  mode,
  section,
  sectionDirty,
  sectionDraft,
}: {
  activeSectionId?: BackendId | null
  mode: Exclude<BuilderPreviewMode, 'readiness'>
  section: TeacherTestSectionRDO
  sectionDirty: boolean
  sectionDraft: BuilderPreviewSectionDraft
}) {
  const instructionsHtml = richTextPreviewHtml(section.instructionsContent)
  const passageHtml = richTextPreviewHtml(section.passageTextContent)
  const isActiveDirtySection = sectionDirty && section.id === activeSectionId
  const paragraphs = orderedParagraphs(section)
  const listeningSegments = orderedListeningSegments(section)

  return (
    <section className="test-builder-preview-section">
      <div className="test-builder-preview-section__heading">
        <h3>{section.title || `Section ${section.sectionOrder}`}</h3>
        {section.durationMinutes ? <Badge tone="neutral">{section.durationMinutes} min</Badge> : null}
        {section.audioFileId ? (
          <Badge tone="info">{mode === 'author' ? `Audio #${section.audioFileId}` : 'Audio ready'}</Badge>
        ) : null}
      </div>
      {isActiveDirtySection && (
        <Alert title="Unsaved section overlay" variant="warning">
          The blocks marked below come from your local active section draft. Save the section for
          the server-backed preview to become authoritative.
        </Alert>
      )}
      {isActiveDirtySection && sectionDraft.instructions ? (
        <div className="test-builder-unsaved-preview">
          <Badge tone="warning">Unsaved instructions</Badge>
          <RichContent blocks={[{ text: sectionDraft.instructions, type: 'paragraph' }]} />
        </div>
      ) : instructionsHtml ? (
        <RichContent trustedHtml={instructionsHtml} />
      ) : null}
      {paragraphs.length > 0 ? (
        <div className="test-builder-preview-paragraphs">
          {paragraphs.map((paragraph) => (
            <section className="reading-paragraph" key={paragraph.id}>
              {paragraph.label && <Badge tone="info">{paragraph.label}</Badge>}
              {paragraph.renderedHtml ? (
                <RichContent trustedHtml={paragraph.renderedHtml} />
              ) : (
                <RichContent
                  blocks={[{ text: paragraph.contentText ?? paragraph.content ?? '', type: 'paragraph' }]}
                />
              )}
            </section>
          ))}
        </div>
      ) : isActiveDirtySection && sectionDraft.passageText ? (
        <div className="test-builder-unsaved-preview">
          <Badge tone="warning">Unsaved passage</Badge>
          <RichContent blocks={[{ text: sectionDraft.passageText, type: 'paragraph' }]} />
        </div>
      ) : passageHtml ? (
        <RichContent trustedHtml={passageHtml} />
      ) : null}
      {mode === 'author' && listeningSegments.length > 0 && (
        <div className="test-builder-preview-note">
          <p className="test-builder-muted">Listening segments</p>
          <ul className="test-builder-validation-list">
            {listeningSegments.map((segment) => (
              <li key={segment.id}>
                <strong>{segment.title}</strong>
                {segment.startSeconds !== undefined && segment.endSeconds !== undefined
                  ? ` (${segment.startSeconds}s-${segment.endSeconds}s)`
                  : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
      {orderedQuestions(section).length > 0 && (
        <ol className="test-builder-preview-questions">
          {orderedQuestions(section).map((question) => (
            <QuestionPreview key={question.id} mode={mode} question={question} />
          ))}
        </ol>
      )}
    </section>
  )
}

export function BuilderPreviewPanel({
  activeSectionId,
  fieldErrors,
  hasMetadataDraft,
  mode,
  onModeChange,
  readinessReport,
  sectionDirty,
  sectionDraft,
  test,
  metadata,
}: {
  activeSectionId?: BackendId | null
  fieldErrors: ApiFieldError[]
  hasMetadataDraft: boolean
  mode: BuilderPreviewMode
  onModeChange: (mode: BuilderPreviewMode) => void
  readinessReport: BuilderReadinessReport
  sectionDirty: boolean
  sectionDraft: BuilderPreviewSectionDraft
  test?: TeacherTestRDO | null
  metadata: BuilderPreviewMetadata
}) {
  const sections = orderedSections(test)
  const renderMode = mode === 'readiness' ? 'author' : mode

  return (
    <Panel
      className="test-builder-preview"
      description="Saved server preview is authoritative; local overlays are marked when unsaved edits exist."
      title="Preview"
    >
      <div className="test-builder-preview__mode" role="group" aria-label="Preview mode">
        <Button
          onClick={() => onModeChange('student')}
          size="sm"
          variant={mode === 'student' ? 'primary' : 'secondary'}
        >
          Student
        </Button>
        <Button
          onClick={() => onModeChange('author')}
          size="sm"
          variant={mode === 'author' ? 'primary' : 'secondary'}
        >
          Answer key
        </Button>
        <Button
          onClick={() => onModeChange('readiness')}
          size="sm"
          variant={mode === 'readiness' ? 'primary' : 'secondary'}
        >
          Readiness
        </Button>
      </div>
      <div className="test-builder-preview__body">
        {mode === 'readiness' ? (
          <ReadinessSummary fieldErrors={fieldErrors} report={readinessReport} />
        ) : null}
        <div className="test-builder-preview-metadata">
          <div className="test-builder-preview-metadata__header">
            <Badge tone="info">{humanizeToken(String(metadata.type))}</Badge>
            {hasMetadataDraft && <Badge tone="warning">Unsaved metadata</Badge>}
          </div>
          <h2>{metadata.title || 'Untitled test'}</h2>
          {metadata.description && <p>{metadata.description}</p>}
          <DescriptionList
            items={[
              { label: 'Difficulty', value: humanizeToken(metadata.difficulty) },
              { label: 'Band', value: `${metadata.estimatedBandMin || '-'} to ${metadata.estimatedBandMax || '-'}` },
              { label: 'Duration', value: `${metadata.durationMinutes || '-'} min` },
              { label: 'Price', value: metadata.free ? 'Free' : `${metadata.priceAmount || 0} ${metadata.currency}` },
            ]}
          />
        </div>
        {sections.length === 0 ? (
          <EmptyState
            eyebrow="Preview"
            message="Saved sections will appear here. Unsaved metadata is reflected above and marked."
            title="No saved sections"
          />
        ) : (
          sections.map((section) => (
            <SectionPreview
              activeSectionId={activeSectionId}
              key={section.id}
              mode={renderMode}
              section={section}
              sectionDirty={sectionDirty}
              sectionDraft={sectionDraft}
            />
          ))
        )}
      </div>
    </Panel>
  )
}
