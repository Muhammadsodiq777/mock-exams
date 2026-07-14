import { useMemo } from 'react'

import { Button, RichContent, TextareaField } from '../ui'
import type { RichTextContentRDO } from '../../types/backend'
import {
  richTextPreviewHtml,
  toPlainRichTextValue,
  type PlainRichTextValue,
} from '../../utils/richText'

type RichTextPlainEditorProps = {
  backendContent?: RichTextContentRDO | null
  disabled?: boolean
  error?: string
  helpText?: string
  id?: string
  label: string
  onChange: (value: PlainRichTextValue) => void
  required?: boolean
  value: string
}

const insertAtLineStart = (value: string, prefix: string) => {
  if (!value.trim()) {
    return prefix
  }

  return value
    .split('\n')
    .map((line) => (line.trim() ? `${prefix}${line}` : line))
    .join('\n')
}

export function RichTextPlainEditor({
  backendContent,
  disabled = false,
  error,
  helpText,
  id,
  label,
  onChange,
  required,
  value,
}: RichTextPlainEditorProps) {
  const previewBlocks = useMemo(
    () =>
      value
        .split(/\n{2,}/)
        .map((line) => line.trim())
        .filter(Boolean)
        .map((text) => ({ text, type: 'paragraph' as const })),
    [value],
  )
  const backendHtml = richTextPreviewHtml(backendContent)

  const updateText = (text: string) => onChange(toPlainRichTextValue(text))

  return (
    <div className="test-builder-rich-text">
      <div className="test-builder-rich-text__toolbar" aria-label={`${label} formatting`}>
        <Button
          aria-label="Start heading line"
          disabled={disabled}
          onClick={() => updateText(insertAtLineStart(value, '## '))}
          size="sm"
          variant="secondary"
        >
          H
        </Button>
        <Button
          aria-label="Start bullet line"
          disabled={disabled}
          onClick={() => updateText(insertAtLineStart(value, '- '))}
          size="sm"
          variant="secondary"
        >
          List
        </Button>
        <Button
          aria-label="Start quote line"
          disabled={disabled}
          onClick={() => updateText(insertAtLineStart(value, '> '))}
          size="sm"
          variant="secondary"
        >
          Quote
        </Button>
      </div>
      <TextareaField
        disabled={disabled}
        error={error}
        helpText={helpText}
        id={id}
        label={label}
        onChange={(event) => updateText(event.target.value)}
        required={required}
        value={value}
      />
      <div className="test-builder-rich-text__preview" aria-label={`${label} preview`}>
        <p className="test-builder-muted">Preview</p>
        {backendHtml && !value ? (
          <RichContent trustedHtml={backendHtml} />
        ) : previewBlocks.length > 0 ? (
          <RichContent blocks={previewBlocks} />
        ) : (
          <p className="test-builder-muted">No content yet.</p>
        )}
      </div>
    </div>
  )
}
