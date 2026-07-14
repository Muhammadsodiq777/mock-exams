import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  AdminPaymentDisputeListRequest,
  AdminPaymentListRequest,
  AdminWithdrawalListRequest,
  AdminWithdrawalRequestRDO,
  BackendId,
  PaymentDisputeDecisionCDO,
  WithdrawalRejectionCDO,
} from '../types/backend'
import { adminKeys } from './queryKeys'

type RejectWithdrawalInput = {
  payload: WithdrawalRejectionCDO
  withdrawalRequestId: BackendId
}

type DecideDisputeInput = {
  disputeId: BackendId
  payload: PaymentDisputeDecisionCDO
}

export function useAdminPayments(filters?: AdminPaymentListRequest) {
  return useQuery({
    queryKey: adminKeys.payments(filters),
    queryFn: () => backendService.admin.listPayments(filters),
  })
}

export function useAdminPayment(paymentId?: BackendId) {
  return useQuery({
    queryKey: paymentId ? adminKeys.payment(paymentId) : adminKeys.payment(0),
    queryFn: () => backendService.admin.getPayment(paymentId as BackendId),
    enabled: Boolean(paymentId),
  })
}

export function useAdminWithdrawals(filters?: AdminWithdrawalListRequest) {
  return useQuery({
    queryKey: adminKeys.withdrawals(filters),
    queryFn: () => backendService.admin.listWithdrawals(filters),
  })
}

export function useAdminWithdrawal(withdrawalRequestId?: BackendId) {
  return useQuery({
    queryKey: withdrawalRequestId
      ? adminKeys.withdrawal(withdrawalRequestId)
      : adminKeys.withdrawal(0),
    queryFn: () => backendService.admin.getWithdrawal(withdrawalRequestId as BackendId),
    enabled: Boolean(withdrawalRequestId),
  })
}

export function useAdminWithdrawalDecisions() {
  const queryClient = useQueryClient()

  const invalidateWithdrawals = (withdrawal: AdminWithdrawalRequestRDO) => {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['admin', 'withdrawals'] })
    void queryClient.invalidateQueries({ queryKey: adminKeys.withdrawal(withdrawal.id) })
  }

  const approve = useMutation({
    mutationFn: (withdrawalRequestId: BackendId) =>
      backendService.admin.approveWithdrawal(withdrawalRequestId),
    onSuccess: invalidateWithdrawals,
  })

  const reject = useMutation({
    mutationFn: ({ payload, withdrawalRequestId }: RejectWithdrawalInput) =>
      backendService.admin.rejectWithdrawal(withdrawalRequestId, payload),
    onSuccess: invalidateWithdrawals,
  })

  const markPaid = useMutation({
    mutationFn: (withdrawalRequestId: BackendId) =>
      backendService.admin.markWithdrawalPaid(withdrawalRequestId),
    onSuccess: invalidateWithdrawals,
  })

  return { approve, markPaid, reject }
}

export function useAdminPaymentDisputes(filters?: AdminPaymentDisputeListRequest) {
  return useQuery({
    queryKey: adminKeys.paymentDisputes(filters),
    queryFn: () => backendService.admin.listPaymentDisputes(filters),
  })
}

export function useAdminPaymentDispute(disputeId?: BackendId) {
  return useQuery({
    queryKey: disputeId ? adminKeys.paymentDispute(disputeId) : adminKeys.paymentDispute(0),
    queryFn: () => backendService.admin.getPaymentDispute(disputeId as BackendId),
    enabled: Boolean(disputeId),
  })
}

export function useAdminPaymentDisputeDecision() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ disputeId, payload }: DecideDisputeInput) =>
      backendService.admin.decidePaymentDispute(disputeId, payload),
    onSuccess: (dispute) => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'payment-disputes'] })
      void queryClient.invalidateQueries({ queryKey: adminKeys.paymentDispute(dispute.id) })
      void queryClient.invalidateQueries({ queryKey: adminKeys.payment(dispute.paymentId) })
    },
  })
}
