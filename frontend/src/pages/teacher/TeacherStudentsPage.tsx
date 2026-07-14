import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  DataTable,
  FilterBar,
  PageContainer,
  PageHeader,
  Pagination,
  Panel,
  SelectField,
  TextField,
} from '../../components/ui'
import { ROUTES, fillRoute } from '../../constants/routes'
import {
  type TeacherStudentActivityRequest,
  useTeacherStudentActivity,
} from '../../hooks/useTeacherStudents'
import type { TeacherReviewRDO } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value && value.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string, fallback: number) => {
  const value = firstParam(params, key)
  if (!value) {
    return fallback
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)

  if (value !== undefined && value !== '') {
    params.set(key, String(value))
  }
}

function useStudentActivityFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<TeacherStudentActivityRequest>(
    () => ({
      page: numberParam(searchParams, 'page', 0),
      rating: numberParam(searchParams, 'rating', 0) || undefined,
      size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
      sort: firstParam(searchParams, 'sort') ?? 'createdAt,desc',
      testId: numberParam(searchParams, 'testId', 0) || undefined,
    }),
    [searchParams],
  )

  const updateFilters = (updates: Partial<TeacherStudentActivityRequest>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)

      Object.entries(updates).forEach(([key, value]) => {
        setParam(next, key, value as number | string | undefined)
      })

      return next
    })
  }

  const clearFilters = () => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      ;['page', 'rating', 'sort', 'testId'].forEach((key) => next.delete(key))
      return next
    })
  }

  return { clearFilters, filters, updateFilters }
}

const reviewSummary = (review: TeacherReviewRDO) =>
  review.comment && review.comment.trim() ? review.comment : 'No written comment'

export function TeacherStudentsPage() {
  const { clearFilters, filters, updateFilters } = useStudentActivityFilters()
  const activityQuery = useTeacherStudentActivity(filters)
  const page = activityQuery.data?.meta.page
  const reviews = activityQuery.data?.items ?? []

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Teacher" title="Students" />

      <Alert title="Student result contract" variant="info">
        The current API contract exposes teacher-owned student review activity, but not a dedicated
        teacher attempt/result list endpoint yet. This page uses the documented teacher-owned
        activity boundary and avoids showing unrelated student data.
      </Alert>

      <Panel title="Student activity" description="Reviews and visible activity for your own content.">
        <FilterBar onClear={clearFilters}>
          <TextField
            label="Test id"
            min={1}
            onChange={(event) =>
              updateFilters({
                page: 0,
                testId: event.target.value ? Number(event.target.value) : undefined,
              })
            }
            type="number"
            value={filters.testId ?? ''}
          />
          <SelectField
            label="Rating"
            onChange={(event) =>
              updateFilters({
                page: 0,
                rating: event.target.value ? Number(event.target.value) : undefined,
              })
            }
            value={filters.rating ?? ''}
          >
            <option value="">All ratings</option>
            <option value="5">5 stars</option>
            <option value="4">4 stars</option>
            <option value="3">3 stars</option>
            <option value="2">2 stars</option>
            <option value="1">1 star</option>
          </SelectField>
          <SelectField
            label="Sort"
            onChange={(event) => updateFilters({ page: 0, sort: event.target.value })}
            value={String(filters.sort ?? 'createdAt,desc')}
          >
            <option value="createdAt,desc">Newest activity</option>
            <option value="createdAt,asc">Oldest activity</option>
            <option value="rating,desc">Highest rating</option>
            <option value="rating,asc">Lowest rating</option>
          </SelectField>
        </FilterBar>

        <DataTable<TeacherReviewRDO>
          columns={[
            {
              header: 'Student',
              key: 'student',
              render: (row) => row.studentDisplayName ?? 'Student',
            },
            {
              header: 'Test',
              key: 'test',
              render: (row) => (
                <Link
                  className="feature-link"
                  to={fillRoute(ROUTES.teacherTestEdit, { id: row.testId })}
                >
                  {row.testTitle}
                </Link>
              ),
            },
            {
              header: 'Rating',
              key: 'rating',
              render: (row) => <Badge tone="info">{row.rating} stars</Badge>,
            },
            { header: 'Comment', key: 'comment', render: reviewSummary },
            { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
          ]}
          emptyMessage="Student activity for your published content will appear here."
          emptyTitle="No student activity"
          error={
            activityQuery.isError
              ? normalizeApiError(activityQuery.error).message
              : undefined
          }
          getRowKey={(row) => row.id}
          isLoading={activityQuery.isLoading}
          rows={reviews}
        />

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={activityQuery.isFetching}
            onNext={() => updateFilters({ page: (filters.page ?? 0) + 1 })}
            onPrevious={() => updateFilters({ page: Math.max((filters.page ?? 0) - 1, 0) })}
            page={page.number + 1}
            pageSize={page.size}
            totalItems={page.totalElements}
            totalPages={page.totalPages}
          />
        )}
      </Panel>

      <Panel title="Result details" description="Attempt/result inspection depends on backend support.">
        <div className="feature-panel-body">
          <p className="feature-card__summary">
            {humanizeToken('teacher_result_list')} is not in the current API contract. Once the
            backend exposes a teacher-owned result list, this screen can add result rows and links
            to allowed attempt detail views without changing the route.
          </p>
        </div>
      </Panel>
    </PageContainer>
  )
}
