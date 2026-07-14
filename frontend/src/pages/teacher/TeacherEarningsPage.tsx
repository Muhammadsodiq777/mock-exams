import { useMemo, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'

import {
  Alert,
  Badge,
  Button,
  DataTable,
  FilterBar,
  PageContainer,
  PageHeader,
  Pagination,
  Panel,
  RepeatedItemCard,
  SelectField,
  TextField,
} from '../../components/ui'
import {
  useTeacherWallet,
  useTeacherWalletTransactions,
  useTeacherWithdrawalMutations,
  useTeacherWithdrawals,
} from '../../hooks/useTeacherEarnings'
import type {
  PageRequest,
  TeacherWalletTransactionRDO,
  TeacherWalletTransactionSourceType,
  TeacherWalletTransactionType,
  TeacherWithdrawalListRequest,
  WithdrawalPayoutMethod,
  WithdrawalRequestCDO,
  WithdrawalRequestRDO,
  WithdrawalRequestStatus,
} from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatCurrency, formatDate, humanizeToken } from '../../utils/formatters'

const DEFAULT_PAGE_SIZE = 20
const MIN_WITHDRAWAL_AMOUNT = 10000

type TransactionFilters = PageRequest & {
  sourceType?: TeacherWalletTransactionSourceType
  transactionType?: TeacherWalletTransactionType
}

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

const transactionTone = (type: string) => {
  if (type === 'earning') {
    return 'success'
  }

  if (type === 'withdrawal' || type === 'refund') {
    return 'warning'
  }

  return 'neutral'
}

const withdrawalTone = (status: string) => {
  if (status === 'paid' || status === 'approved') {
    return 'success'
  }

  if (status === 'rejected' || status === 'cancelled') {
    return 'danger'
  }

  return 'warning'
}

const getPayoutSummary = (withdrawal: WithdrawalRequestRDO) => {
  const details = withdrawal.payoutDetails as Record<string, string | undefined>
  const method = String(details.method ?? '')

  if (method === 'card') {
    return `Card ${details.cardNumber ?? ''}`.trim()
  }

  if (method === 'bank_transfer') {
    return `${details.bankName ?? 'Bank'} ${details.accountNumber ?? ''}`.trim()
  }

  return humanizeToken(method)
}

function useEarningsFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const transactionFilters = useMemo<TransactionFilters>(
    () => ({
      page: numberParam(searchParams, 'txPage', 0),
      size: numberParam(searchParams, 'txSize', DEFAULT_PAGE_SIZE),
      sort: firstParam(searchParams, 'txSort') ?? 'createdAt,desc',
      sourceType: firstParam(searchParams, 'sourceType') as
        | TeacherWalletTransactionSourceType
        | undefined,
      transactionType: firstParam(searchParams, 'transactionType') as
        | TeacherWalletTransactionType
        | undefined,
    }),
    [searchParams],
  )

  const withdrawalFilters = useMemo<TeacherWithdrawalListRequest>(
    () => ({
      page: numberParam(searchParams, 'withdrawalPage', 0),
      size: numberParam(searchParams, 'withdrawalSize', DEFAULT_PAGE_SIZE),
      sort: firstParam(searchParams, 'withdrawalSort') ?? 'createdAt,desc',
      status: firstParam(searchParams, 'withdrawalStatus') as
        | WithdrawalRequestStatus
        | undefined,
    }),
    [searchParams],
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
        'sourceType',
        'transactionType',
        'txPage',
        'txSort',
        'withdrawalPage',
        'withdrawalStatus',
        'withdrawalSort',
      ].forEach((key) => next.delete(key))
      return next
    })
  }

  return { clearFilters, transactionFilters, updateFilters, withdrawalFilters }
}

function WithdrawalForm({ availableAmount, currency }: { availableAmount: number; currency: string }) {
  const { createWithdrawal } = useTeacherWithdrawalMutations()
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState<WithdrawalPayoutMethod>('card')
  const [cardNumber, setCardNumber] = useState('')
  const [cardHolder, setCardHolder] = useState('')
  const [bankName, setBankName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [accountHolder, setAccountHolder] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)
  const apiError = createWithdrawal.isError ? normalizeApiError(createWithdrawal.error) : null
  const parsedAmount = Number(amount)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLocalError(null)

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setLocalError('Enter a withdrawal amount greater than zero.')
      return
    }

    if (parsedAmount < MIN_WITHDRAWAL_AMOUNT) {
      setLocalError(
        `Minimum withdrawal is ${formatCurrency(MIN_WITHDRAWAL_AMOUNT, currency, '10000 UZS')}.`,
      )
      return
    }

    if (availableAmount > 0 && parsedAmount > availableAmount) {
      setLocalError('Amount cannot exceed the available wallet balance.')
      return
    }

    const payoutDetails: WithdrawalRequestCDO['payoutDetails'] =
      method === 'card'
        ? { cardHolder, cardNumber, method }
        : { accountHolder, accountNumber, bankName, method }

    createWithdrawal.mutate(
      { amount: parsedAmount, currency, payoutDetails },
      {
        onSuccess: () => {
          setAmount('')
          setCardNumber('')
          setCardHolder('')
          setBankName('')
          setAccountNumber('')
          setAccountHolder('')
        },
      },
    )
  }

  return (
    <form className="feature-stack" onSubmit={submit}>
      {(localError || apiError) && (
        <Alert title="Withdrawal request needs attention" variant="danger">
          {localError ?? apiError?.message}
        </Alert>
      )}
      <TextField
        label="Amount"
        min={0}
        onChange={(event) => setAmount(event.target.value)}
        required
        step="0.01"
        type="number"
        value={amount}
      />
      <SelectField
        label="Payout method"
        onChange={(event) => setMethod(event.target.value as WithdrawalPayoutMethod)}
        value={method}
      >
        <option value="card">Card</option>
        <option value="bank_transfer">Bank transfer</option>
      </SelectField>
      {method === 'card' ? (
        <>
          <TextField
            label="Card number"
            onChange={(event) => setCardNumber(event.target.value)}
            required
            value={cardNumber}
          />
          <TextField
            label="Card holder"
            onChange={(event) => setCardHolder(event.target.value)}
            required
            value={cardHolder}
          />
        </>
      ) : (
        <>
          <TextField
            label="Bank name"
            onChange={(event) => setBankName(event.target.value)}
            required
            value={bankName}
          />
          <TextField
            label="Account number"
            onChange={(event) => setAccountNumber(event.target.value)}
            required
            value={accountNumber}
          />
          <TextField
            label="Account holder"
            onChange={(event) => setAccountHolder(event.target.value)}
            required
            value={accountHolder}
          />
        </>
      )}
      <Button isLoading={createWithdrawal.isPending} loadingLabel="Requesting" type="submit">
        Request withdrawal
      </Button>
    </form>
  )
}

function WalletSummary({
  availableAmount,
  currency,
  pendingAmount,
  walletLoaded,
}: {
  availableAmount: number
  currency: string
  pendingAmount: number
  walletLoaded: boolean
}) {
  return (
    <div className="feature-card-grid feature-card-grid--compact">
      <RepeatedItemCard>
        <Badge tone="success">Wallet</Badge>
        <h3>Available</h3>
        <p className="dashboard-metric">
          {walletLoaded ? formatCurrency(availableAmount, currency, '0 UZS') : 'Not available'}
        </p>
      </RepeatedItemCard>
      <RepeatedItemCard>
        <Badge tone="warning">Reserved</Badge>
        <h3>Pending withdrawals</h3>
        <p className="dashboard-metric">
          {walletLoaded ? formatCurrency(pendingAmount, currency, '0 UZS') : 'Not available'}
        </p>
      </RepeatedItemCard>
    </div>
  )
}

export function TeacherEarningsPage() {
  const filters = useEarningsFilters()
  const walletQuery = useTeacherWallet()
  const transactionsQuery = useTeacherWalletTransactions(filters.transactionFilters)
  const withdrawalsQuery = useTeacherWithdrawals(filters.withdrawalFilters)
  const { cancelWithdrawal } = useTeacherWithdrawalMutations()
  const wallet = walletQuery.data
  const currency = wallet?.currency ?? 'UZS'
  const availableAmount = Number(wallet?.availableAmount ?? wallet?.balanceAmount ?? 0)
  const pendingAmount = Number(wallet?.pendingAmount ?? 0)
  const transactions = transactionsQuery.data?.items ?? []
  const withdrawals = withdrawalsQuery.data?.items ?? []
  const transactionPage = transactionsQuery.data?.meta.page
  const withdrawalPage = withdrawalsQuery.data?.meta.page

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Teacher" title="Earnings" />

      {walletQuery.isError && (
        <Alert title="Wallet could not be loaded" variant="danger">
          {normalizeApiError(walletQuery.error).message}
        </Alert>
      )}

      <Panel title="Wallet summary" description="Balances come directly from the teacher wallet API.">
        <WalletSummary
          availableAmount={availableAmount}
          currency={currency}
          pendingAmount={pendingAmount}
          walletLoaded={Boolean(wallet)}
        />
      </Panel>

      <div className="dashboard-grid">
        <Panel
          title="Request withdrawal"
          description={`Minimum request is ${formatCurrency(MIN_WITHDRAWAL_AMOUNT, currency, '10000 UZS')}.`}
        >
          <div className="feature-panel-body">
            <WithdrawalForm availableAmount={availableAmount} currency={currency} />
          </div>
        </Panel>

        <Panel title="Withdrawal history" description="Requested payouts and review outcomes.">
          <FilterBar
            onClear={() => filters.updateFilters({ withdrawalPage: 0, withdrawalStatus: undefined })}
          >
            <SelectField
              label="Status"
              onChange={(event) =>
                filters.updateFilters({
                  withdrawalPage: 0,
                  withdrawalStatus: event.target.value || undefined,
                })
              }
              value={filters.withdrawalFilters.status ?? ''}
            >
              <option value="">All statuses</option>
              <option value="requested">Requested</option>
              <option value="approved">Approved</option>
              <option value="rejected">Rejected</option>
              <option value="paid">Paid</option>
              <option value="cancelled">Cancelled</option>
            </SelectField>
          </FilterBar>
          <DataTable
            columns={[
              {
                header: 'Status',
                key: 'status',
                render: (row) => <Badge tone={withdrawalTone(row.status)}>{humanizeToken(row.status)}</Badge>,
              },
              {
                header: 'Amount',
                key: 'amount',
                render: (row) => formatCurrency(row.requestedAmount, row.currency, '0 UZS'),
              },
              { header: 'Payout', key: 'payout', render: getPayoutSummary },
              { header: 'Requested', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
              {
                header: 'Reason',
                key: 'reason',
                render: (row) => row.rejectionReason ?? 'None',
              },
              {
                header: 'Actions',
                key: 'actions',
                render: (row) =>
                  row.status === 'requested' ? (
                    <Button
                      isLoading={cancelWithdrawal.isPending}
                      onClick={() => cancelWithdrawal.mutate(row.id)}
                      size="sm"
                      variant="secondary"
                    >
                      Cancel
                    </Button>
                  ) : (
                    'No action'
                  ),
              },
            ]}
            emptyMessage="Withdrawal requests will appear here after you submit one."
            emptyTitle="No withdrawals"
            error={
              withdrawalsQuery.isError
                ? normalizeApiError(withdrawalsQuery.error).message
                : undefined
            }
            getRowKey={(row) => row.id}
            isLoading={withdrawalsQuery.isLoading}
            rows={withdrawals}
          />
          {withdrawalPage && (
            <Pagination
              canGoNext={!withdrawalPage.last}
              canGoPrevious={!withdrawalPage.first}
              isLoading={withdrawalsQuery.isFetching}
              onNext={() =>
                filters.updateFilters({ withdrawalPage: filters.withdrawalFilters.page! + 1 })
              }
              onPrevious={() =>
                filters.updateFilters({
                  withdrawalPage: Math.max((filters.withdrawalFilters.page ?? 0) - 1, 0),
                })
              }
              page={withdrawalPage.number + 1}
              pageSize={withdrawalPage.size}
              totalItems={withdrawalPage.totalElements}
              totalPages={withdrawalPage.totalPages}
            />
          )}
        </Panel>
      </div>

      <Panel title="Transactions" description="Marketplace, withdrawal, and future live-speaking ledger rows.">
        <FilterBar onClear={filters.clearFilters}>
          <SelectField
            label="Type"
            onChange={(event) =>
              filters.updateFilters({
                transactionType: event.target.value || undefined,
                txPage: 0,
              })
            }
            value={filters.transactionFilters.transactionType ?? ''}
          >
            <option value="">All types</option>
            <option value="earning">Earning</option>
            <option value="withdrawal">Withdrawal</option>
            <option value="refund">Refund</option>
            <option value="adjustment">Adjustment</option>
          </SelectField>
          <SelectField
            label="Source"
            onChange={(event) =>
              filters.updateFilters({
                sourceType: event.target.value || undefined,
                txPage: 0,
              })
            }
            value={filters.transactionFilters.sourceType ?? ''}
          >
            <option value="">All sources</option>
            <option value="marketplace_purchase">Marketplace purchase</option>
            <option value="withdrawal_request">Withdrawal request</option>
            <option value="live_speaking_booking">Live speaking booking</option>
            <option value="refund">Refund</option>
            <option value="adjustment">Adjustment</option>
          </SelectField>
        </FilterBar>
        <DataTable<TeacherWalletTransactionRDO>
          columns={[
            {
              header: 'Type',
              key: 'type',
              render: (row) => (
                <Badge tone={transactionTone(row.transactionType)}>
                  {humanizeToken(row.transactionType)}
                </Badge>
              ),
            },
            { header: 'Source', key: 'source', render: (row) => humanizeToken(row.sourceType) },
            {
              align: 'right',
              header: 'Gross',
              key: 'gross',
              render: (row) => formatCurrency(row.grossAmount, row.currency, '0 UZS'),
            },
            {
              align: 'right',
              header: 'Commission',
              key: 'commission',
              render: (row) => formatCurrency(row.platformCommissionAmount, row.currency, '0 UZS'),
            },
            {
              align: 'right',
              header: 'Net',
              key: 'net',
              render: (row) => formatCurrency(row.netAmount, row.currency, '0 UZS'),
            },
            { header: 'Created', key: 'createdAt', render: (row) => formatDate(row.createdAt) },
            { header: 'Description', key: 'description', render: (row) => row.description ?? 'None' },
          ]}
          emptyMessage="No wallet transactions match the selected filters."
          emptyTitle="No transactions"
          error={
            transactionsQuery.isError
              ? normalizeApiError(transactionsQuery.error).message
              : undefined
          }
          getRowKey={(row) => row.id}
          isLoading={transactionsQuery.isLoading}
          rows={transactions}
        />
        {transactionPage && (
          <Pagination
            canGoNext={!transactionPage.last}
            canGoPrevious={!transactionPage.first}
            isLoading={transactionsQuery.isFetching}
            onNext={() =>
              filters.updateFilters({ txPage: (filters.transactionFilters.page ?? 0) + 1 })
            }
            onPrevious={() =>
              filters.updateFilters({
                txPage: Math.max((filters.transactionFilters.page ?? 0) - 1, 0),
              })
            }
            page={transactionPage.number + 1}
            pageSize={transactionPage.size}
            totalItems={transactionPage.totalElements}
            totalPages={transactionPage.totalPages}
          />
        )}
      </Panel>
    </PageContainer>
  )
}
