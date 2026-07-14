export const formatNumber = (value?: number | string | null, fallback = 'Not measured') => {
  if (value === undefined || value === null || value === '') {
    return fallback
  }

  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(numeric % 1 === 0 ? 0 : 1) : String(value)
}

export const formatCurrency = (
  amount?: number | string | null,
  currency = 'UZS',
  fallback = 'Free',
) => {
  if (amount === undefined || amount === null || Number(amount) === 0) {
    return fallback
  }

  const numeric = Number(amount)
  if (!Number.isFinite(numeric)) {
    return `${amount} ${currency}`
  }

  try {
    return new Intl.NumberFormat(undefined, {
      currency,
      maximumFractionDigits: 0,
      style: 'currency',
    }).format(numeric)
  } catch {
    return `${numeric.toLocaleString()} ${currency}`
  }
}

export const formatDate = (value?: string | null, fallback = 'Not available') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.valueOf())) {
    return fallback
  }

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date)
}

export const formatMinutes = (minutes?: number | null, fallback = 'Untimed') => {
  if (!minutes) {
    return fallback
  }

  return `${minutes} min`
}

export const formatSeconds = (seconds?: number | null, fallback = 'Not tracked') => {
  if (seconds === undefined || seconds === null) {
    return fallback
  }

  const minutes = Math.floor(seconds / 60)
  const remaining = seconds % 60
  return `${minutes}:${String(remaining).padStart(2, '0')}`
}

export const humanizeToken = (value?: string | null, fallback = 'Unknown') => {
  if (!value) {
    return fallback
  }

  return value
    .split(/[_-]/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
