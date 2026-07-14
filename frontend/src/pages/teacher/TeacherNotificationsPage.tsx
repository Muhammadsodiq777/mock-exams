import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Pagination,
  Panel,
  RepeatedItemCard,
} from '../../components/ui'
import {
  useMarkTeacherNotificationRead,
  useTeacherNotifications,
} from '../../hooks/useTeacherNotifications'
import type { BackendId, PageRequest } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatDate } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20

const firstParam = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value?.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string, fallback: number) => {
  const parsed = Number(firstParam(params, key))
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const setParam = (params: URLSearchParams, key: string, value?: number | string) => {
  params.delete(key)

  if (value !== undefined && value !== '') {
    params.set(key, String(value))
  }
}

const isInternalPath = (value?: string | null) => Boolean(value?.startsWith('/'))

export function TeacherNotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = useMemo<PageRequest>(
    () => ({
      page: numberParam(searchParams, 'page', 0),
      size: numberParam(searchParams, 'size', DEFAULT_PAGE_SIZE),
      sort: firstParam(searchParams, 'sort') ?? 'createdAt,desc',
    }),
    [searchParams],
  )
  const notificationsQuery = useTeacherNotifications(filters)
  const markRead = useMarkTeacherNotificationRead()
  const notifications = notificationsQuery.data?.items ?? []
  const page = notificationsQuery.data?.meta.page

  const updateFilters = (updates: Record<string, number | string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(updates).forEach(([key, value]) => setParam(next, key, value))
      return next
    })
  }

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Teacher" title="Notifications" />

      <Panel title="Inbox" description="Account, moderation, and student activity updates.">
        {notificationsQuery.isLoading && (
          <div className="feature-panel-state">
            <Loader label="Loading notifications" />
          </div>
        )}

        {notificationsQuery.isError && (
          <div className="feature-panel-state">
            <Alert title="Notifications could not be loaded" variant="danger">
              {normalizeApiError(notificationsQuery.error).message}
            </Alert>
          </div>
        )}

        {!notificationsQuery.isLoading && !notificationsQuery.isError && notifications.length === 0 && (
          <EmptyState
            className="feature-panel-state"
            title="No notifications"
            message="Moderation decisions, student activity, and account updates will appear here."
          />
        )}

        {!notificationsQuery.isLoading && !notificationsQuery.isError && notifications.length > 0 && (
          <div className="feature-list feature-list--padded">
            {notifications.map((notification) => (
              <RepeatedItemCard
                className={notification.read ? undefined : 'notification-card--unread'}
                key={notification.id}
              >
                <div className="feature-card__badges">
                  <Badge tone={notification.read ? 'neutral' : 'warning'}>
                    {notification.read ? 'Read' : 'Unread'}
                  </Badge>
                  <Badge tone="info">{formatDate(notification.createdAt)}</Badge>
                </div>
                <h3>{notification.title}</h3>
                <p className="feature-card__summary">{notification.body}</p>
                <div className="feature-card__actions">
                  {isInternalPath(notification.targetPath) && (
                    <Link className="feature-link" to={notification.targetPath as string}>
                      Open target
                    </Link>
                  )}
                  {!notification.read && (
                    <Button
                      isLoading={
                        markRead.isPending &&
                        markRead.variables === (notification.id as BackendId)
                      }
                      onClick={() => markRead.mutate(notification.id)}
                      size="sm"
                      variant="secondary"
                    >
                      Mark read
                    </Button>
                  )}
                </div>
              </RepeatedItemCard>
            ))}
          </div>
        )}

        {page && (
          <Pagination
            canGoNext={!page.last}
            canGoPrevious={!page.first}
            isLoading={notificationsQuery.isFetching}
            onNext={() => updateFilters({ page: page.number + 1 })}
            onPrevious={() => updateFilters({ page: Math.max(0, page.number - 1) })}
            page={page.number + 1}
            pageSize={page.size}
            totalItems={page.totalElements}
            totalPages={page.totalPages}
          />
        )}
      </Panel>
    </PageContainer>
  )
}
