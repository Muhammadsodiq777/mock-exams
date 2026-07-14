import { EmptyState, PageContainer } from '../components/ui'

type PlaceholderPageProps = {
  eyebrow: string
  title: string
  summary: string
}

export function PlaceholderPage({ eyebrow, summary, title }: PlaceholderPageProps) {
  return (
    <PageContainer>
      <EmptyState
        className="placeholder-panel"
        eyebrow={eyebrow}
        message={summary}
        title={title}
      />
    </PageContainer>
  )
}
