import type { RichTextContentRDO, RichTextDocument } from '../types/backend'

export type PlainRichTextValue = {
  document: RichTextDocument
  text: string
}

export const emptyRichTextDocument = (): RichTextDocument => ({
  version: 1,
  type: 'doc',
  content: [],
})

export function plainTextToRichTextDocument(text: string): RichTextDocument {
  const paragraphs = text
    .split(/\n{2,}/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => ({
      type: 'paragraph',
      content: [{ type: 'text', text: line }],
    }))

  return {
    version: 1,
    type: 'doc',
    content: paragraphs,
  }
}

export function toPlainRichTextValue(text: string): PlainRichTextValue {
  return {
    document: text.trim() ? plainTextToRichTextDocument(text) : emptyRichTextDocument(),
    text,
  }
}

export function richTextPreviewHtml(content?: RichTextContentRDO | null) {
  return content?.renderedHtml || undefined
}
