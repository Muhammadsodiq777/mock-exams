import { Button } from './Button'

export type PaginationProps = {
  canGoNext: boolean
  canGoPrevious: boolean
  isLoading?: boolean
  label?: string
  onNext: () => void
  onPrevious: () => void
  page: number
  pageSize?: number
  totalItems?: number
  totalPages?: number
}

export function Pagination({
  canGoNext,
  canGoPrevious,
  isLoading = false,
  label = 'Pagination',
  onNext,
  onPrevious,
  page,
  pageSize,
  totalItems,
  totalPages,
}: PaginationProps) {
  const summary =
    totalPages !== undefined
      ? `Page ${page} of ${totalPages}`
      : totalItems !== undefined && pageSize !== undefined
        ? `${totalItems} items, ${pageSize} per page`
        : `Page ${page}`

  return (
    <nav className="ui-pagination" aria-label={label}>
      <p>{summary}</p>
      <div className="ui-pagination__actions">
        <Button
          disabled={!canGoPrevious}
          isLoading={isLoading}
          onClick={onPrevious}
          size="sm"
          variant="secondary"
        >
          Previous
        </Button>
        <Button
          disabled={!canGoNext}
          isLoading={isLoading}
          onClick={onNext}
          size="sm"
          variant="secondary"
        >
          Next
        </Button>
      </div>
    </nav>
  )
}
