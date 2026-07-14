import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'

import type {
  MarketplaceListingRequest,
  SortQueryValue,
  TestCatalogRequest,
  TestPriceFilter,
  TestType,
} from '../types/backend'

type UrlValue = number | string | undefined

const DEFAULT_PAGE_SIZE = 12

const first = (params: URLSearchParams, key: string) => {
  const value = params.get(key)
  return value && value.trim() ? value.trim() : undefined
}

const numberParam = (params: URLSearchParams, key: string) => {
  const value = first(params, key)
  if (!value) {
    return undefined
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const allValues = (params: URLSearchParams, keys: string[]) => {
  const values = keys.flatMap((key) => params.getAll(key))
  const splitValues = values.flatMap((value) => value.split(','))
  const normalized = splitValues.map((value) => value.trim()).filter(Boolean)
  return normalized.length ? normalized : undefined
}

const setValue = (next: URLSearchParams, key: string, value: UrlValue) => {
  next.delete(key)

  if (value === undefined || value === '') {
    return
  }

  next.set(key, String(value))
}

export function useTestCatalogFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<TestCatalogRequest>(() => {
    const tags = allValues(searchParams, ['tag', 'tags'])
    const categories = allValues(searchParams, ['category', 'categories'])

    return {
      page: numberParam(searchParams, 'page') ?? 0,
      size: numberParam(searchParams, 'size') ?? DEFAULT_PAGE_SIZE,
      sort: (first(searchParams, 'sort') as SortQueryValue | undefined) ?? 'publishedAt,desc',
      type: first(searchParams, 'type') as TestType | undefined,
      difficulty: first(searchParams, 'difficulty'),
      bandMin: first(searchParams, 'bandMin'),
      bandMax: first(searchParams, 'bandMax'),
      language: first(searchParams, 'language'),
      price: first(searchParams, 'price') as TestPriceFilter | undefined,
      teacherId: numberParam(searchParams, 'teacherId'),
      tags,
      categories,
    }
  }, [searchParams])

  const updateFilters = (updates: Partial<TestCatalogRequest>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)

      Object.entries(updates).forEach(([key, value]) => {
        if (Array.isArray(value)) {
          next.delete(key)
          value.filter(Boolean).forEach((item) => next.append(key, String(item)))
          return
        }

        setValue(next, key, value as UrlValue)
      })

      return next
    })
  }

  return { filters, updateFilters }
}

export function useMarketplaceFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = useMemo<MarketplaceListingRequest>(() => {
    const tags = allValues(searchParams, ['tag', 'tags'])
    const categories = allValues(searchParams, ['category', 'categories'])

    return {
      page: numberParam(searchParams, 'page') ?? 0,
      size: numberParam(searchParams, 'size') ?? DEFAULT_PAGE_SIZE,
      sort: (first(searchParams, 'sort') as SortQueryValue | undefined) ?? 'createdAt,desc',
      q: first(searchParams, 'q'),
      type: first(searchParams, 'type') as MarketplaceListingRequest['type'],
      language: first(searchParams, 'language'),
      price: first(searchParams, 'price') as TestPriceFilter | undefined,
      minPrice: first(searchParams, 'minPrice'),
      maxPrice: first(searchParams, 'maxPrice'),
      teacherId: numberParam(searchParams, 'teacherId'),
      tags,
      categories,
    }
  }, [searchParams])

  const updateFilters = (updates: Partial<MarketplaceListingRequest>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)

      Object.entries(updates).forEach(([key, value]) => {
        if (Array.isArray(value)) {
          next.delete(key)
          value.filter(Boolean).forEach((item) => next.append(key, String(item)))
          return
        }

        setValue(next, key, value as UrlValue)
      })

      return next
    })
  }

  return { filters, updateFilters }
}

