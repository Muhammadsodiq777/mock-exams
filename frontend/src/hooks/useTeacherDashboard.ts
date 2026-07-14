import { useCallback } from 'react'

import { useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  ApiDashboardResult,
  TeacherDashboardRDO,
  TeacherProfileRDO,
  TeacherWalletTransactionRDO,
} from '../types/backend'
import { normalizeApiError } from '../utils/apiErrors'
import { teacherKeys } from './queryKeys'

const isMissingDashboardEndpoint = (error: unknown) => normalizeApiError(error).status === 404

const toDashboardProfile = (profile: TeacherProfileRDO): TeacherDashboardRDO['profile'] => ({
  id: profile.id,
  displayName: profile.displayName,
  status: profile.status,
  profileComplete: Boolean(profile.displayName && profile.bio),
  rejectionReason: profile.rejectionReason,
  approvedAt: profile.approvedAt,
  updatedAt: profile.updatedAt,
})

const isEarningTransaction = (transaction: TeacherWalletTransactionRDO) =>
  transaction.transactionType === 'earning'

const toNumber = (value?: number | string | null) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

async function optional<T>(task: Promise<T>) {
  try {
    return await task
  } catch {
    return null
  }
}

async function getFallbackTeacherDashboard(): Promise<ApiDashboardResult<TeacherDashboardRDO>> {
  const profile = await backendService.teacher.getProfile()

  if (profile.status !== 'approved') {
    return {
      dashboard: {
        profile: toDashboardProfile(profile),
        state: 'profile_limited',
      },
      meta: {
        requestId: 'frontend-fallback',
        stale: true,
        generatedBy: 'frontend-fallback',
      },
    }
  }

  const [wallet, transactionsResult, reviewSummary, unreadCount] = await Promise.all([
    optional(backendService.teacher.getWallet()),
    optional(backendService.teacher.listWalletTransactions({ page: 0, size: 10 })),
    optional(backendService.teacher.getReviewSummary()),
    optional(backendService.teacher.getNotificationUnreadCount()),
  ])
  const transactions = transactionsResult?.items ?? []
  const earningTransactions = transactions.filter(isEarningTransaction)
  const recentSalesAmount = earningTransactions.reduce(
    (total, transaction) => total + toNumber(transaction.grossAmount),
    0,
  )

  return {
    dashboard: {
      profile: toDashboardProfile(profile),
      state: 'fallback',
      tests: null,
      wallet,
      sales: transactionsResult
        ? {
            currency: wallet?.currency ?? earningTransactions[0]?.currency ?? 'UZS',
            latestSaleAt: earningTransactions[0]?.createdAt ?? null,
            recentSalesAmount,
            recentSalesCount: earningTransactions.length,
          }
        : null,
      attempts: null,
      reviews: reviewSummary
        ? {
            averageRating: reviewSummary.ratingAverage,
            ratingCount: reviewSummary.ratingCount,
            recentReviewCount: reviewSummary.ratingCount,
          }
        : null,
      notifications: unreadCount
        ? {
            latestNotificationAt: null,
            unreadCount: unreadCount.unreadCount,
          }
        : null,
      schedule: null,
    },
    meta: {
      requestId: 'frontend-fallback',
      stale: true,
      generatedBy: 'frontend-fallback',
    },
  }
}

async function getTeacherDashboard() {
  try {
    return await backendService.teacher.getDashboard()
  } catch (error) {
    if (isMissingDashboardEndpoint(error)) {
      return getFallbackTeacherDashboard()
    }

    throw error
  }
}

export function useTeacherDashboard() {
  return useQuery({
    queryKey: teacherKeys.dashboard,
    queryFn: getTeacherDashboard,
  })
}

export function useInvalidateTeacherDashboard() {
  const queryClient = useQueryClient()

  return useCallback(
    () => queryClient.invalidateQueries({ queryKey: teacherKeys.dashboard }),
    [queryClient],
  )
}
