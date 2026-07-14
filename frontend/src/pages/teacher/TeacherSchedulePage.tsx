import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  DataTable,
  DescriptionList,
  EmptyState,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  TextField,
} from '../../components/ui'
import { ROUTES } from '../../constants/routes'
import {
  useTeacherBookingActions,
  useTeacherScheduleBookings,
  useTeacherScheduleSlots,
  useTeacherSlotMutations,
} from '../../hooks/useLiveSpeakingSchedule'
import type { LiveSpeakingBookingRDO, LiveSpeakingSlotRDO } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatCurrency, humanizeToken } from '../../utils/formatters'

const defaultPage = { page: 0, size: 20, sort: 'startsAt,asc' }

const localInputValue = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

const defaultStartsAt = () => {
  const next = new Date()
  next.setDate(next.getDate() + 1)
  next.setMinutes(0, 0, 0)
  return localInputValue(next)
}

const defaultEndsAt = () => {
  const next = new Date()
  next.setDate(next.getDate() + 1)
  next.setHours(next.getHours() + 1)
  next.setMinutes(0, 0, 0)
  return localInputValue(next)
}

const formatLocalDateTime = (value?: string | null, fallback = 'Not scheduled') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.valueOf())) {
    return fallback
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

const statusTone = (status: string) => {
  if (status === 'announced' || status === 'booked' || status === 'completed') {
    return 'success'
  }

  if (status === 'reserved' || status === 'draft') {
    return 'warning'
  }

  if (status === 'cancelled' || status === 'expired' || status === 'no_show') {
    return 'danger'
  }

  return 'neutral'
}

function SlotForm({ onCreated }: { onCreated: (message: string) => void }) {
  const { createSlot } = useTeacherSlotMutations()
  const [startsAt, setStartsAt] = useState(defaultStartsAt)
  const [endsAt, setEndsAt] = useState(defaultEndsAt)
  const [timezone, setTimezone] = useState(
    Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
  )
  const [priceAmount, setPriceAmount] = useState('100000')
  const [currency, setCurrency] = useState('UZS')
  const [localError, setLocalError] = useState<string | null>(null)
  const apiError = createSlot.isError ? normalizeApiError(createSlot.error) : null

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLocalError(null)

    const startDate = new Date(startsAt)
    const endDate = new Date(endsAt)
    const price = Number(priceAmount)

    if (Number.isNaN(startDate.valueOf()) || Number.isNaN(endDate.valueOf())) {
      setLocalError('Choose valid start and end times.')
      return
    }

    if (endDate <= startDate) {
      setLocalError('End time must be after start time.')
      return
    }

    if (!Number.isFinite(price) || price < 0) {
      setLocalError('Enter a valid non-negative price.')
      return
    }

    createSlot.mutate(
      {
        currency: currency.trim().toUpperCase(),
        endsAt: endDate.toISOString(),
        priceAmount: price,
        startsAt: startDate.toISOString(),
        timezone: timezone.trim() || 'UTC',
      },
      {
        onSuccess: (slot) => {
          onCreated(`Slot ${slot.id} created for ${formatLocalDateTime(slot.startsAt)}.`)
          setStartsAt(defaultStartsAt())
          setEndsAt(defaultEndsAt())
        },
      },
    )
  }

  return (
    <Panel title="Create speaking slot" description="Add one bookable live speaking session.">
      <form className="feature-panel-body" onSubmit={submit}>
        {(localError || apiError) && (
          <Alert title="Slot could not be created" variant="danger">
            {localError ?? apiError?.message}
          </Alert>
        )}
        <div className="feature-card-grid feature-card-grid--compact">
          <TextField
            label="Starts"
            onChange={(event) => setStartsAt(event.target.value)}
            required
            type="datetime-local"
            value={startsAt}
          />
          <TextField
            label="Ends"
            onChange={(event) => setEndsAt(event.target.value)}
            required
            type="datetime-local"
            value={endsAt}
          />
          <TextField
            label="Timezone"
            onChange={(event) => setTimezone(event.target.value)}
            required
            value={timezone}
          />
          <TextField
            label="Price"
            min={0}
            onChange={(event) => setPriceAmount(event.target.value)}
            required
            type="number"
            value={priceAmount}
          />
          <TextField
            label="Currency"
            maxLength={3}
            onChange={(event) => setCurrency(event.target.value.toUpperCase())}
            required
            value={currency}
          />
        </div>
        <Button isLoading={createSlot.isPending} loadingLabel="Creating" type="submit">
          Create slot
        </Button>
      </form>
    </Panel>
  )
}

function SlotsPanel({ onAction }: { onAction: (message: string) => void }) {
  const slotsQuery = useTeacherScheduleSlots(defaultPage)
  const { announceSlot, unannounceSlot } = useTeacherSlotMutations()

  const runVisibilityAction = (slot: LiveSpeakingSlotRDO) => {
    const mutation = slot.announced ? unannounceSlot : announceSlot
    mutation.mutate(slot.id, {
      onSuccess: () =>
        onAction(
          slot.announced
            ? `Slot ${slot.id} is no longer announced.`
            : `Slot ${slot.id} is now announced to students.`,
        ),
    })
  }

  return (
    <Panel title="Your slots" description="Announced slots are visible to students.">
      <DataTable<LiveSpeakingSlotRDO>
        columns={[
          {
            header: 'Time',
            key: 'time',
            render: (row) => (
              <div className="profile-table-stack">
                <strong>{formatLocalDateTime(row.startsAt)}</strong>
                <span>Ends {formatLocalDateTime(row.endsAt)}</span>
              </div>
            ),
          },
          {
            header: 'Status',
            key: 'status',
            render: (row) => <Badge tone={statusTone(row.status)}>{humanizeToken(row.status)}</Badge>,
          },
          {
            header: 'Price',
            key: 'price',
            render: (row) => formatCurrency(row.priceAmount, row.currency, 'Free'),
          },
          { header: 'Timezone', key: 'timezone', render: (row) => row.timezone },
          {
            header: 'Actions',
            key: 'actions',
            render: (row) => {
              const isBusy = announceSlot.isPending || unannounceSlot.isPending
              const canToggle = row.status === 'draft' || row.status === 'announced'

              return (
                <Button
                  disabled={!canToggle}
                  isLoading={isBusy}
                  loadingLabel="Saving"
                  onClick={() => runVisibilityAction(row)}
                  size="sm"
                  variant={row.announced ? 'secondary' : 'primary'}
                >
                  {row.announced ? 'Unannounce' : 'Announce'}
                </Button>
              )
            },
          },
        ]}
        emptyMessage="Create a slot to start offering live speaking sessions."
        emptyTitle="No slots yet"
        error={slotsQuery.isError ? normalizeApiError(slotsQuery.error).message : undefined}
        getRowKey={(row) => row.id}
        isLoading={slotsQuery.isLoading}
        rows={slotsQuery.data?.items ?? []}
      />
    </Panel>
  )
}

function BookingCard({
  booking,
  onAction,
}: {
  booking: LiveSpeakingBookingRDO
  onAction: (message: string) => void
}) {
  const { completeBooking, markNoShow } = useTeacherBookingActions()
  const canClose = booking.status === 'booked' || booking.status === 'reserved'

  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone={statusTone(booking.status)}>{humanizeToken(booking.status)}</Badge>
        <Badge tone={booking.meetUrl ? 'success' : 'warning'}>
          {booking.meetUrl ? 'Meet ready' : 'Meet pending'}
        </Badge>
      </div>
      <DescriptionList
        items={[
          { label: 'Student', value: booking.studentDisplayName ?? booking.studentUserId },
          { label: 'Starts', value: formatLocalDateTime(booking.startsAt) },
          { label: 'Price', value: formatCurrency(booking.priceAmount, booking.currency, 'Free') },
          { label: 'Payment', value: booking.paymentStatus ? humanizeToken(booking.paymentStatus) : booking.paymentId ?? 'None' },
          { label: 'Provider sync', value: humanizeToken(String(booking.providerSyncStatus ?? 'pending')) },
        ]}
      />
      {booking.providerFailureMessage && (
        <Alert title="Provider issue" variant="warning">
          {booking.providerFailureMessage}
        </Alert>
      )}
      {booking.meetUrl && (
        <a className="feature-link" href={booking.meetUrl} rel="noreferrer" target="_blank">
          Open Meet link
        </a>
      )}
      <div className="feature-card__actions">
        <Button
          disabled={!canClose}
          isLoading={completeBooking.isPending}
          loadingLabel="Completing"
          onClick={() =>
            completeBooking.mutate(booking.id, {
              onSuccess: () => onAction(`Booking ${booking.id} marked complete.`),
            })
          }
          size="sm"
        >
          Complete
        </Button>
        <Button
          disabled={!canClose}
          isLoading={markNoShow.isPending}
          loadingLabel="Saving"
          onClick={() =>
            markNoShow.mutate(booking.id, {
              onSuccess: () => onAction(`Booking ${booking.id} marked no-show.`),
            })
          }
          size="sm"
          variant="secondary"
        >
          No-show
        </Button>
      </div>
    </RepeatedItemCard>
  )
}

function BookingsPanel({ onAction }: { onAction: (message: string) => void }) {
  const bookingsQuery = useTeacherScheduleBookings(defaultPage)
  const bookings = useMemo(
    () => bookingsQuery.data?.items ?? [],
    [bookingsQuery.data?.items],
  )

  return (
    <Panel title="Upcoming bookings" description="Confirmed and reserved student sessions.">
      <div className="feature-panel-body">
        {bookingsQuery.isLoading && <p className="status">Loading bookings...</p>}
        {bookingsQuery.isError && (
          <Alert title="Bookings could not be loaded" variant="danger">
            {normalizeApiError(bookingsQuery.error).message}
          </Alert>
        )}
        {!bookingsQuery.isLoading && !bookingsQuery.isError && !bookings.length && (
          <EmptyState
            title="No bookings yet"
            message="Student reservations and paid sessions will appear here."
          />
        )}
        {bookings.length > 0 && (
          <div className="feature-list">
            {bookings.map((booking) => (
              <BookingCard booking={booking} key={booking.id} onAction={onAction} />
            ))}
          </div>
        )}
      </div>
    </Panel>
  )
}

export function TeacherSchedulePage() {
  const [message, setMessage] = useState<string | null>(null)

  const showMessage = (nextMessage: string) => {
    setMessage(nextMessage)
  }

  return (
    <PageContainer size="wide">
      <PageHeader
        actions={
          <Link className="feature-link" to={ROUTES.teacherDashboard}>
            Dashboard
          </Link>
        }
        eyebrow="Teacher"
        title="Schedule"
      />

      {message && (
        <Alert title="Schedule updated" variant="success">
          {message}
        </Alert>
      )}

      <div className="writing-layout">
        <SlotForm onCreated={showMessage} />
        <BookingsPanel onAction={showMessage} />
      </div>

      <SlotsPanel onAction={showMessage} />
    </PageContainer>
  )
}
