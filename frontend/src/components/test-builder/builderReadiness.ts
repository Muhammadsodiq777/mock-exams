import type { ApiFieldError, TeacherTestRDO, TeacherTestSectionRDO, TestType } from '../../types/backend'
import { humanizeToken } from '../../utils/formatters'

export type BuilderPreviewMetadata = {
  title: string
  description: string
  type: TestType | string
  difficulty: string
  estimatedBandMin: string
  estimatedBandMax: string
  durationMinutes: string
  priceAmount: string
  currency: string
  free: boolean
  language: string
}

export type BuilderReadinessStatus = 'ready' | 'warning' | 'blocked'

export type BuilderReadinessItem = {
  id: string
  label: string
  detail: string
  status: BuilderReadinessStatus
}

export type BuilderReadinessReport = {
  items: BuilderReadinessItem[]
  readyToSubmit: boolean
  blockedCount: number
  warningCount: number
}

const objectiveQuestionTypes = new Set([
  'multiple_choice',
  'matching_information',
  'true_false_not_given',
  'yes_no_not_given',
  'sentence_completion',
  'summary_completion',
  'table_completion',
  'diagram_labeling',
  'short_answer',
  'gap_fill',
  'ordering',
])

const orderedSections = (test?: TeacherTestRDO | null) =>
  [...(test?.sections ?? [])].sort((left, right) => left.sectionOrder - right.sectionOrder)

const orderedQuestions = (section: TeacherTestSectionRDO) =>
  [...(section.questions ?? [])].sort((left, right) => left.questionOrder - right.questionOrder)

const orderedParagraphs = (section: TeacherTestSectionRDO) =>
  [...(section.paragraphs ?? [])].sort((left, right) => left.paragraphOrder - right.paragraphOrder)

const hasText = (value?: string | null) => Boolean(value?.trim())

const contentHasText = (content?: { contentText?: string | null } | null) =>
  hasText(content?.contentText)

const sectionHasPassageContent = (section: TeacherTestSectionRDO) =>
  contentHasText(section.passageTextContent) ||
  hasText(section.passageText) ||
  orderedParagraphs(section).some((paragraph) =>
    hasText(paragraph.contentText ?? paragraph.content),
  )

const sectionHasInstructions = (section: TeacherTestSectionRDO) =>
  contentHasText(section.instructionsContent) || hasText(section.instructions)

const isListeningRelevant = (test: TeacherTestRDO, sections: TeacherTestSectionRDO[]) =>
  test.type === 'listening' ||
  sections.some((section) => (section.listeningSegments ?? []).length > 0)

const statusItem = (
  id: string,
  label: string,
  status: BuilderReadinessStatus,
  detail: string,
): BuilderReadinessItem => ({ detail, id, label, status })

const numericValue = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const parsed = Number(trimmed)
  return Number.isFinite(parsed) ? parsed : null
}

export function buildBuilderReadinessReport({
  fieldErrors,
  hasUnsavedChanges,
  metadata,
  test,
}: {
  fieldErrors: ApiFieldError[]
  hasUnsavedChanges: boolean
  metadata: BuilderPreviewMetadata
  test?: TeacherTestRDO | null
}): BuilderReadinessReport {
  const items: BuilderReadinessItem[] = []

  if (!test) {
    items.push(
      statusItem(
        'draft',
        'Saved draft',
        'blocked',
        'Save metadata once so the server can create an authoritative draft preview.',
      ),
    )
  }

  items.push(
    statusItem(
      'unsaved',
      'Unsaved edits',
      hasUnsavedChanges ? 'blocked' : 'ready',
      hasUnsavedChanges
        ? 'Save metadata and the active section before submitting.'
        : 'The preview is using saved server content.',
    ),
  )

  const hasCoreMetadata =
    hasText(metadata.title) &&
    hasText(metadata.type) &&
    hasText(metadata.difficulty) &&
    hasText(metadata.language) &&
    numericValue(metadata.durationMinutes) !== null

  items.push(
    statusItem(
      'metadata',
      'Metadata',
      hasCoreMetadata ? 'ready' : 'blocked',
      hasCoreMetadata
        ? 'Title, type, difficulty, duration, and language are present.'
        : 'Add title, type, difficulty, duration, and language before submission.',
    ),
  )

  const priceReady =
    metadata.free ||
    (numericValue(metadata.priceAmount) !== null && hasText(metadata.currency))

  items.push(
    statusItem(
      'price',
      'Price and visibility',
      priceReady ? 'ready' : 'blocked',
      priceReady
        ? metadata.free
          ? 'The test is marked free.'
          : `The saved price is ${metadata.priceAmount || 0} ${metadata.currency || 'currency pending'}.`
        : 'Paid tests need a numeric price and currency.',
    ),
  )

  if (test) {
    const sections = orderedSections(test)
    const readingSectionCountReady = test.type === 'reading' ? sections.length === 3 : sections.length > 0
    const sectionCountDetail =
      test.type === 'reading'
        ? `Reading tests need three saved sections; this draft has ${sections.length}.`
        : `This draft has ${sections.length} saved section${sections.length === 1 ? '' : 's'}.`

    items.push(
      statusItem(
        'sections',
        'Sections',
        readingSectionCountReady ? 'ready' : 'blocked',
        sectionCountDetail,
      ),
    )

    const sectionsHaveContent =
      sections.length > 0 &&
      sections.every((section) => sectionHasInstructions(section) || sectionHasPassageContent(section))
    const readingContentReady =
      test.type !== 'reading' ||
      (sections.length > 0 && sections.every((section) => sectionHasPassageContent(section)))
    const sectionContentStatus: BuilderReadinessStatus =
      test.type === 'reading'
        ? readingContentReady ? 'ready' : 'blocked'
        : sectionsHaveContent ? 'ready' : 'warning'

    items.push(
      statusItem(
        'section-content',
        'Section content',
        sectionContentStatus,
        test.type === 'reading'
          ? readingContentReady
            ? 'Every Reading section has meaningful saved passage content.'
            : 'Reading sections need meaningful saved passage content.'
          : sectionsHaveContent
            ? 'Saved section instructions or passage content are present.'
            : 'Section instructions or passage content are optional for this test type, but useful for teacher/admin preview.',
      ),
    )

    const listeningRelevant = isListeningRelevant(test, sections)
    const listeningReady =
      !listeningRelevant ||
      (sections.length > 0 &&
        sections.every((section) => Boolean(section.audioFileId)))

    items.push(
      statusItem(
        'listening-audio',
        'Listening audio',
        listeningReady ? 'ready' : 'blocked',
        listeningRelevant
          ? listeningReady
            ? 'Every listening-relevant section has a saved audio file id.'
            : 'Listening sections need saved audio file ids before submission.'
          : 'No saved listening segments require section audio yet.',
      ),
    )

    const questionSectionsReady =
      sections.length > 0 && sections.every((section) => orderedQuestions(section).length > 0)

    items.push(
      statusItem(
        'questions',
        'Questions',
        questionSectionsReady ? 'ready' : 'blocked',
        questionSectionsReady
          ? 'Every saved section has at least one question.'
          : 'Add at least one saved question to each section.',
      ),
    )

    const objectiveQuestions = sections.flatMap((section) =>
      orderedQuestions(section).filter((question) =>
        objectiveQuestionTypes.has(question.questionType),
      ),
    )
    const answerKeysReady = objectiveQuestions.every(
      (question) => (question.answerKeys ?? []).length > 0,
    )

    items.push(
      statusItem(
        'answer-keys',
        'Objective answer keys',
        answerKeysReady ? 'ready' : 'blocked',
        answerKeysReady
          ? 'Saved objective questions have answer keys.'
          : 'Objective questions need at least one saved answer key.',
      ),
    )

    const submittableStatus = test.visibility === 'draft' || test.visibility === 'rejected'
    items.push(
      statusItem(
        'status',
        'Moderation status',
        submittableStatus ? 'ready' : 'blocked',
        submittableStatus
          ? test.visibility === 'rejected'
            ? 'Rejected tests are editable and can be resubmitted after changes.'
            : 'Draft tests can be submitted for moderation.'
          : `${humanizeToken(String(test.visibility))} tests are read-only for teachers.`,
      ),
    )

    if (test.visibility === 'rejected') {
      items.push(
        statusItem(
          'moderation-note',
          'Rejected note',
          hasText(test.moderationNote) ? 'warning' : 'blocked',
          hasText(test.moderationNote)
            ? 'The moderation note is visible to guide resubmission.'
            : 'The backend did not return a moderation note for this rejected test.',
        ),
      )
    }
  }

  if (fieldErrors.length > 0) {
    items.push(
      statusItem(
        'backend-fields',
        'Backend validation',
        'blocked',
        `${fieldErrors.length} server validation issue${fieldErrors.length === 1 ? '' : 's'} must be resolved.`,
      ),
    )
  }

  const blockedCount = items.filter((item) => item.status === 'blocked').length
  const warningCount = items.filter((item) => item.status === 'warning').length

  return {
    blockedCount,
    items,
    readyToSubmit: blockedCount === 0,
    warningCount,
  }
}
