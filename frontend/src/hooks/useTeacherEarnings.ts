import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  BackendId,
  PageRequest,
  TeacherWithdrawalListRequest,
  WithdrawalRequestCDO,
} from '../types/backend'
import { teacherKeys } from './queryKeys'

export function useTeacherWallet() {
  return useQuery({
    queryKey: teacherKeys.wallet,
    queryFn: () => backendService.teacher.getWallet(),
  })
}

export function useTeacherWalletTransactions(filters?: PageRequest) {
  return useQuery({
    queryKey: teacherKeys.walletTransactions(filters),
    queryFn: () => backendService.teacher.listWalletTransactions(filters),
  })
}

export function useTeacherWithdrawals(filters?: TeacherWithdrawalListRequest) {
  return useQuery({
    queryKey: teacherKeys.withdrawals(filters),
    queryFn: () => backendService.teacher.listWithdrawals(filters),
  })
}

export function useTeacherWithdrawal(withdrawalRequestId?: BackendId) {
  return useQuery({
    enabled: Boolean(withdrawalRequestId),
    queryKey: withdrawalRequestId
      ? teacherKeys.withdrawal(withdrawalRequestId)
      : teacherKeys.withdrawal(0),
    queryFn: () => backendService.teacher.getWithdrawal(withdrawalRequestId as BackendId),
  })
}

export function useTeacherWithdrawalMutations() {
  const queryClient = useQueryClient()

  const invalidateWalletState = () => {
    void queryClient.invalidateQueries({ queryKey: teacherKeys.wallet })
    void queryClient.invalidateQueries({ queryKey: teacherKeys.withdrawalsRoot })
    void queryClient.invalidateQueries({ queryKey: teacherKeys.walletTransactionsRoot })
    void queryClient.invalidateQueries({ queryKey: teacherKeys.dashboard })
  }

  const createWithdrawal = useMutation({
    mutationFn: (payload: WithdrawalRequestCDO) =>
      backendService.teacher.createWithdrawal(payload),
    onSuccess: (withdrawal) => {
      queryClient.setQueryData(teacherKeys.withdrawal(withdrawal.id), withdrawal)
      invalidateWalletState()
    },
  })

  const cancelWithdrawal = useMutation({
    mutationFn: (withdrawalRequestId: BackendId) =>
      backendService.teacher.cancelWithdrawal(withdrawalRequestId),
    onSuccess: (withdrawal) => {
      queryClient.setQueryData(teacherKeys.withdrawal(withdrawal.id), withdrawal)
      invalidateWalletState()
    },
  })

  return {
    cancelWithdrawal,
    createWithdrawal,
  }
}
