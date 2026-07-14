import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  DataTable,
  DescriptionList,
  EmptyState,
  FilterBar,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  SelectField,
  Tabs,
  TextField,
} from '../../components/ui'
import { useAdminReport } from '../../hooks/useAdminReports'
import type { AdminReportName, AdminReportRequest } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatCurrency, formatDate, formatNumber, humanizeToken } from '../../utils/formatters'

const reportOptions: Array<{ label: string; value: AdminReportName }> = [
  { label: 'AI usage', value: 'ai-usage' },
  { label: 'Sales', value: 'sales' },
  { label: 'Revenue', value: 'revenue' },
  { label: 'Test activity', value: 'test-activity' },
  { label: 'Provider failures', value: 'provider-failures' },
]

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value?.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string) => {
  const value = firstParam(params, key)
  if (!value) {
    return undefined
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined
}

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)
  if (value !== undefined && value !== '' && value !== 'all') {
    params.set(key, String(value))
  }
}

const startOfDayIso = (value?: string) => (value ? `${value}T00:00:00.000Z` : undefined)
const endOfDayIso = (value?: string) => (value ? `${value}T23:59:59.999Z` : undefined)

const isCurrencyField = (key: string) =>
  /(amount|cost|gross|net|commission|revenue|price)/i.test(key)

const isDateField = (key: string) => /(date|at|generated)/i.test(key)

const formatReportValue = (key: string, value: number | string | null) => {
  if (value === null || value === undefined || value === '') {
    return 'None'
  }

  if (isDateField(key) && typeof value === 'string') {
    return formatDate(value)
  }

  if (isCurrencyField(key) && (typeof value === 'number' || /^-?\d+(\.\d+)?$/.test(String(value)))) {
    return formatCurrency(value, 'UZS', '0 UZS')
  }

  if (typeof value === 'number') {
    return formatNumber(value, '0')
  }

  return String(value)
}

function useReportFilters() {
  const [searchParams, setSearchParams] = useSearchParams()
  const reportParam = firstParam(searchParams, 'report')
  const activeReport = reportOptions.some((option) => option.value === reportParam)
    ? (reportParam as AdminReportName)
    : 'ai-usage'
  const from = firstParam(searchParams, 'from')
  const to = firstParam(searchParams, 'to')
  const rangeInvalid =
    from && to
      ? new Date(`${to}T23:59:59.999Z`).getTime() < new Date(`${from}T00:00:00.000Z`).getTime()
      : false

  const filters = useMemo<AdminReportRequest>(
    () => ({
      feedbackType: firstParam(searchParams, 'feedbackType'),
      from: startOfDayIso(from),
      listingId: numberParam(searchParams, 'listingId'),
      model: firstParam(searchParams, 'model'),
      paymentStatus: firstParam(searchParams, 'paymentStatus'),
      provider: firstParam(searchParams, 'provider'),
      status: firstParam(searchParams, 'status'),
      studentUserId: numberParam(searchParams, 'studentUserId'),
      teacherProfileId: numberParam(searchParams, 'teacherProfileId'),
      testType: firstParam(searchParams, 'testType'),
      to: endOfDayIso(to),
    }),
    [from, searchParams, to],
  )

  const updateFilters = (updates: Record<string, number | string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(updates).forEach(([key, value]) => setParam(next, key, value))
      return next
    })
  }

  const clearFilters = () => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      ;[
        'feedbackType',
        'from',
        'listingId',
        'model',
        'paymentStatus',
        'provider',
        'status',
        'studentUserId',
        'teacherProfileId',
        'testType',
        'to',
      ].forEach((key) => next.delete(key))
      return next
    })
  }

  return {
    activeReport,
    clearFilters,
    filters,
    from: from ?? '',
    rangeInvalid,
    to: to ?? '',
    updateFilters,
  }
}

function ReportFilters({
  filters,
  from,
  onClear,
  to,
  updateFilters,
}: {
  filters: AdminReportRequest
  from: string
  onClear: () => void
  to: string
  updateFilters: (updates: Record<string, number | string | undefined>) => void
}) {
  return (
    <FilterBar onClear={onClear}>
      <TextField
        label="From"
        onChange={(event) => updateFilters({ from: event.target.value })}
        type="date"
        value={from}
      />
      <TextField
        label="To"
        onChange={(event) => updateFilters({ to: event.target.value })}
        type="date"
        value={to}
      />
      <TextField
        label="Teacher profile id"
        onChange={(event) => updateFilters({ teacherProfileId: event.target.value })}
        type="number"
        value={filters.teacherProfileId ?? ''}
      />
      <TextField
        label="Student user id"
        onChange={(event) => updateFilters({ studentUserId: event.target.value })}
        type="number"
        value={filters.studentUserId ?? ''}
      />
      <TextField
        label="Listing id"
        onChange={(event) => updateFilters({ listingId: event.target.value })}
        type="number"
        value={filters.listingId ?? ''}
      />
      <TextField
        label="Provider"
        onChange={(event) => updateFilters({ provider: event.target.value })}
        value={filters.provider ?? ''}
      />
      <TextField
        label="Model"
        onChange={(event) => updateFilters({ model: event.target.value })}
        value={filters.model ?? ''}
      />
      <TextField
        label="Status"
        onChange={(event) => updateFilters({ status: event.target.value })}
        value={filters.status ?? ''}
      />
      <TextField
        label="Payment status"
        onChange={(event) => updateFilters({ paymentStatus: event.target.value })}
        value={filters.paymentStatus ?? ''}
      />
      <TextField
        label="Feedback type"
        onChange={(event) => updateFilters({ feedbackType: event.target.value })}
        value={filters.feedbackType ?? ''}
      />
      <TextField
        label="Test type"
        onChange={(event) => updateFilters({ testType: event.target.value })}
        value={filters.testType ?? ''}
      />
    </FilterBar>
  )
}

function ReportPanel({
  filters,
  isEnabled,
  name,
}: {
  filters: AdminReportRequest
  isEnabled: boolean
  name: AdminReportName
}) {
  const reportQuery = useAdminReport(name, filters, isEnabled)
  const report = reportQuery.data
  const totalEntries = Object.entries(report?.totals ?? {})
  const rowKeys = Array.from(new Set((report?.rows ?? []).flatMap((row) => Object.keys(row)))).slice(0, 8)

  if (reportQuery.isLoading) {
    return (
      <Panel>
        <div className="feature-panel-state">
          <Loader label="Loading report" />
        </div>
      </Panel>
    )
  }

  if (reportQuery.isError) {
    return (
      <Alert title="Report could not be loaded" variant="danger">
        {normalizeApiError(reportQuery.error).message}
      </Alert>
    )
  }

  if (!report) {
    return null
  }

  return (
    <div className="feature-stack">
      <Alert title="Generated" variant="info">
        {formatDate(report.generatedAt)}. Report totals are backend aggregates; the frontend only
        formats them for review.
      </Alert>
      {report.liveSpeakingStatus && (
        <Alert title="Live speaking" variant="warning">
          {report.liveSpeakingStatus}
        </Alert>
      )}
      <Panel title="Totals">
        {totalEntries.length === 0 ? (
          <div className="feature-panel-state">
            <EmptyState title="No totals" message="The report returned no aggregate totals." />
          </div>
        ) : (
          <div className="feature-card-grid">
            {totalEntries.map(([key, value]) => (
              <RepeatedItemCard key={key}>
                <Badge tone="info">{humanizeToken(key)}</Badge>
                <p className="dashboard-metric">{formatReportValue(key, value)}</p>
              </RepeatedItemCard>
            ))}
          </div>
        )}
      </Panel>
      <Panel title="Rows" description="First columns returned by the selected report endpoint.">
        <DataTable<Record<string, number | string | null>>
          columns={rowKeys.map((key) => ({
            header: humanizeToken(key),
            key,
            render: (row) => formatReportValue(key, row[key]),
          }))}
          emptyMessage="No rows match the selected report filters."
          emptyTitle="No report rows"
          getRowKey={(row) => JSON.stringify(row)}
          rows={report.rows}
        />
      </Panel>
      <Panel title="Effective filters">
        <div className="feature-panel-body">
          <DescriptionList
            items={Object.entries(report.filters).map(([key, value]) => ({
              label: humanizeToken(key),
              value: value === null || value === undefined ? 'None' : String(value),
            }))}
          />
        </div>
      </Panel>
    </div>
  )
}

export function AdminReportsPage() {
  const { activeReport, clearFilters, filters, from, rangeInvalid, to, updateFilters } =
    useReportFilters()

  return (
    <PageContainer size="wide">
      <PageHeader
        eyebrow="Admin"
        title="Reports"
        actions={
          <SelectField
            label="Report"
            onChange={(event) => updateFilters({ report: event.target.value })}
            value={activeReport}
          >
            {reportOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </SelectField>
        }
      />

      <ReportFilters
        filters={filters}
        from={from}
        onClear={clearFilters}
        to={to}
        updateFilters={updateFilters}
      />

      {rangeInvalid && (
        <Alert title="Invalid report date range" variant="danger">
          Choose a start date that is earlier than or equal to the end date.
        </Alert>
      )}

      <Tabs
        onValueChange={(value) => updateFilters({ report: value })}
        value={activeReport}
        items={reportOptions.map((option) => ({
          content: <ReportPanel filters={filters} isEnabled={!rangeInvalid} name={option.value} />,
          id: option.value,
          label: option.label,
        }))}
      />
    </PageContainer>
  )
}
