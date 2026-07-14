import type {
  MarketplaceListingRequest,
  MarketplaceListingType,
  TestCatalogRequest,
  TestPriceFilter,
  TestType,
} from '../../types/backend'
import { Button, SelectField, TextField } from '../ui'

type FilterOption = {
  label: string
  value: string
}

const testTypeOptions: FilterOption[] = [
  { label: 'All types', value: '' },
  { label: 'Reading', value: 'reading' },
  { label: 'Listening', value: 'listening' },
  { label: 'Writing Task 1', value: 'writing_task_1' },
  { label: 'Writing Task 2', value: 'writing_task_2' },
  { label: 'Speaking Part 1', value: 'speaking_part_1' },
  { label: 'Full mock', value: 'full_mock' },
]

const marketplaceTypeOptions: FilterOption[] = [
  { label: 'All content', value: '' },
  { label: 'Single test', value: 'single_test' },
  { label: 'Test pack', value: 'test_pack' },
  { label: 'Writing course', value: 'writing_course' },
  { label: 'Speaking pack', value: 'speaking_question_pack' },
  { label: 'Vocabulary pack', value: 'vocabulary_pack' },
  { label: 'Full mock exam', value: 'full_mock_exam' },
  { label: 'Subscription plan', value: 'subscription_plan' },
]

const priceOptions: FilterOption[] = [
  { label: 'Any price', value: '' },
  { label: 'Free', value: 'free' },
  { label: 'Paid', value: 'paid' },
]

const sortOptions: FilterOption[] = [
  { label: 'Newest', value: 'publishedAt,desc' },
  { label: 'Title A-Z', value: 'title,asc' },
  { label: 'Lowest price', value: 'priceAmount,asc' },
  { label: 'Shortest duration', value: 'durationMinutes,asc' },
]

const marketplaceSortOptions: FilterOption[] = [
  { label: 'Newest', value: 'createdAt,desc' },
  { label: 'Title A-Z', value: 'title,asc' },
  { label: 'Lowest price', value: 'priceAmount,asc' },
]

const splitCsv = (value: string) =>
  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

const csv = (values?: string | string[]) => {
  if (!values) {
    return ''
  }

  return Array.isArray(values) ? values.join(', ') : values
}

export function TestCatalogFilters({
  filters,
  onChange,
}: {
  filters: TestCatalogRequest
  onChange: (updates: Partial<TestCatalogRequest>) => void
}) {
  return (
    <div className="discovery-filters" aria-label="Test filters">
      <SelectField
        label="Type"
        value={filters.type ?? ''}
        onChange={(event) => onChange({ page: 0, type: event.target.value as TestType || undefined })}
      >
        {testTypeOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        label="Price"
        value={filters.price ?? ''}
        onChange={(event) => onChange({ page: 0, price: event.target.value as TestPriceFilter || undefined })}
      >
        {priceOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        label="Sort"
        value={String(filters.sort ?? 'publishedAt,desc')}
        onChange={(event) => onChange({ page: 0, sort: event.target.value })}
      >
        {sortOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <TextField
        label="Difficulty"
        value={filters.difficulty ?? ''}
        onChange={(event) => onChange({ difficulty: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Band from"
        inputMode="decimal"
        value={filters.bandMin ?? ''}
        onChange={(event) => onChange({ bandMin: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Band to"
        inputMode="decimal"
        value={filters.bandMax ?? ''}
        onChange={(event) => onChange({ bandMax: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Tags"
        helpText="Comma-separated slugs"
        value={csv(filters.tags ?? filters.tag)}
        onChange={(event) => onChange({ page: 0, tag: undefined, tags: splitCsv(event.target.value) })}
      />
      <TextField
        label="Categories"
        helpText="Comma-separated slugs"
        value={csv(filters.categories ?? filters.category)}
        onChange={(event) =>
          onChange({ categories: splitCsv(event.target.value), category: undefined, page: 0 })
        }
      />
      <TextField
        label="Language"
        value={filters.language ?? ''}
        onChange={(event) => onChange({ language: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Teacher ID"
        inputMode="numeric"
        value={filters.teacherId ?? ''}
        onChange={(event) => onChange({ page: 0, teacherId: Number(event.target.value) || undefined })}
      />
      <div className="discovery-filters__actions">
        <Button variant="secondary" onClick={() => onChange({ page: 0 })}>
          Refresh
        </Button>
      </div>
    </div>
  )
}

export function MarketplaceFilters({
  filters,
  onChange,
}: {
  filters: MarketplaceListingRequest
  onChange: (updates: Partial<MarketplaceListingRequest>) => void
}) {
  return (
    <div className="discovery-filters" aria-label="Marketplace filters">
      <TextField
        label="Search"
        value={filters.q ?? ''}
        onChange={(event) => onChange({ page: 0, q: event.target.value || undefined })}
      />
      <SelectField
        label="Type"
        value={filters.type ?? ''}
        onChange={(event) =>
          onChange({ page: 0, type: event.target.value as MarketplaceListingType || undefined })
        }
      >
        {marketplaceTypeOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        label="Price"
        value={filters.price ?? ''}
        onChange={(event) => onChange({ page: 0, price: event.target.value as TestPriceFilter || undefined })}
      >
        {priceOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <SelectField
        label="Sort"
        value={String(filters.sort ?? 'createdAt,desc')}
        onChange={(event) => onChange({ page: 0, sort: event.target.value })}
      >
        {marketplaceSortOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </SelectField>
      <TextField
        label="Minimum price"
        inputMode="decimal"
        value={filters.minPrice ?? ''}
        onChange={(event) => onChange({ minPrice: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Maximum price"
        inputMode="decimal"
        value={filters.maxPrice ?? ''}
        onChange={(event) => onChange({ maxPrice: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Tags"
        helpText="Comma-separated slugs"
        value={csv(filters.tags ?? filters.tag)}
        onChange={(event) => onChange({ page: 0, tag: undefined, tags: splitCsv(event.target.value) })}
      />
      <TextField
        label="Categories"
        helpText="Comma-separated slugs"
        value={csv(filters.categories ?? filters.category)}
        onChange={(event) =>
          onChange({ categories: splitCsv(event.target.value), category: undefined, page: 0 })
        }
      />
      <TextField
        label="Language"
        value={filters.language ?? ''}
        onChange={(event) => onChange({ language: event.target.value || undefined, page: 0 })}
      />
      <TextField
        label="Teacher ID"
        inputMode="numeric"
        value={filters.teacherId ?? ''}
        onChange={(event) => onChange({ page: 0, teacherId: Number(event.target.value) || undefined })}
      />
      <div className="discovery-filters__actions">
        <Button variant="secondary" onClick={() => onChange({ page: 0 })}>
          Refresh
        </Button>
      </div>
    </div>
  )
}
