import type { ReactNode } from 'react'

import { Button } from './Button'
import { EmptyState } from './EmptyState'
import { Loader } from './Loader'
import { cx } from './shared'

export type TableColumn<T> = {
  align?: 'left' | 'right'
  header: ReactNode
  key: string
  render: (row: T) => ReactNode
}

export type DataTableProps<T> = {
  columns: Array<TableColumn<T>>
  emptyMessage?: string
  emptyTitle?: string
  error?: string
  getRowKey: (row: T) => string | number
  isLoading?: boolean
  rows: T[]
}

export function DataTable<T>({
  columns,
  emptyMessage = 'No matching records are available.',
  emptyTitle = 'No records',
  error,
  getRowKey,
  isLoading = false,
  rows,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="ui-table-state">
        <Loader label="Loading records" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="ui-table-state">
        <EmptyState eyebrow="Error" title="Unable to load records" message={error} />
      </div>
    )
  }

  if (rows.length === 0) {
    return (
      <div className="ui-table-state">
        <EmptyState title={emptyTitle} message={emptyMessage} />
      </div>
    )
  }

  return (
    <div className="ui-table-wrap">
      <table className="ui-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                className={cx(column.align === 'right' && 'ui-table__cell--right')}
                key={column.key}
                scope="col"
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={getRowKey(row)}>
              {columns.map((column) => (
                <td
                  className={cx(column.align === 'right' && 'ui-table__cell--right')}
                  key={column.key}
                >
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export type AdminTableShellProps = {
  actions?: ReactNode
  children: ReactNode
  description?: string
  filters?: ReactNode
  title: string
}

export function AdminTableShell({
  actions,
  children,
  description,
  filters,
  title,
}: AdminTableShellProps) {
  return (
    <section className="ui-admin-table" aria-label={title}>
      <div className="ui-admin-table__header">
        <div>
          <h2>{title}</h2>
          {description && <p>{description}</p>}
        </div>
        {actions && <div className="ui-admin-table__actions">{actions}</div>}
      </div>
      {filters && <div className="ui-admin-table__filters">{filters}</div>}
      {children}
    </section>
  )
}

export type FilterBarProps = {
  children: ReactNode
  onClear?: () => void
}

export function FilterBar({ children, onClear }: FilterBarProps) {
  return (
    <div className="ui-filter-bar">
      <div className="ui-filter-bar__fields">{children}</div>
      {onClear && (
        <Button onClick={onClear} size="sm" variant="ghost">
          Clear
        </Button>
      )}
    </div>
  )
}
