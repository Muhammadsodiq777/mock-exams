import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { backendService } from '../services/backend.service'
import type {
  BackendId,
  LiveSpeakingSlotCDO,
  LiveSpeakingSlotUDO,
  PageRequest,
} from '../types/backend'
import { marketplaceKeys, scheduleKeys } from './queryKeys'

type UpdateSlotInput = {
  payload: LiveSpeakingSlotUDO
  slotId: BackendId
}

const currentBrowserUrl = () => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.location.href
}

export function isExternalCheckoutUrl(url?: string | null) {
  if (!url) {
    return false
  }

  return url.startsWith('http://') || url.startsWith('https://')
}

export function usePublicScheduleSlots(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.publicSlots(filters),
    queryFn: () => backendService.schedule.listPublicSlots(filters),
  })
}

export function useStudentScheduleBookings(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.studentBookings(filters),
    queryFn: () => backendService.schedule.listStudentBookings(filters),
  })
}

export function useStudentScheduleBooking(bookingId?: BackendId) {
  return useQuery({
    enabled: Boolean(bookingId),
    queryKey: bookingId
      ? scheduleKeys.studentBooking(bookingId)
      : scheduleKeys.studentBooking(0),
    queryFn: () => backendService.schedule.getStudentBooking(bookingId as BackendId),
  })
}

export function useReserveLiveSpeakingSlot() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (slotId: BackendId) => backendService.schedule.reserveSlot(slotId),
    onSuccess: (booking) => {
      queryClient.setQueryData(scheduleKeys.studentBooking(booking.id), booking)
      void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
    },
  })
}

export function useCreateLiveSpeakingBookingPayment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookingId: BackendId) => backendService.schedule.createBookingPayment(bookingId),
    onSuccess: (payment) => {
      void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
      void queryClient.invalidateQueries({ queryKey: marketplaceKeys.payments })
      if (payment.targetLiveSpeakingBookingId) {
        void queryClient.invalidateQueries({
          queryKey: scheduleKeys.studentBooking(payment.targetLiveSpeakingBookingId),
        })
      }
    },
  })
}

export function useCancelLiveSpeakingBooking() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookingId: BackendId) => backendService.schedule.cancelStudentBooking(bookingId),
    onSuccess: (booking) => {
      queryClient.setQueryData(scheduleKeys.studentBooking(booking.id), booking)
      void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
    },
  })
}

export function useTeacherScheduleSlots(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.teacherSlots(filters),
    queryFn: () => backendService.schedule.listTeacherSlots(filters),
  })
}

export function useTeacherScheduleBookings(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.teacherBookings(filters),
    queryFn: () => backendService.schedule.listTeacherBookings(filters),
  })
}

export function useTeacherScheduleBooking(bookingId?: BackendId) {
  return useQuery({
    enabled: Boolean(bookingId),
    queryKey: bookingId
      ? scheduleKeys.teacherBooking(bookingId)
      : scheduleKeys.teacherBooking(0),
    queryFn: () => backendService.schedule.getTeacherBooking(bookingId as BackendId),
  })
}

export function useTeacherSlotMutations() {
  const queryClient = useQueryClient()

  const invalidateSchedule = () => {
    void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
  }

  const createSlot = useMutation({
    mutationFn: (payload: LiveSpeakingSlotCDO) => backendService.schedule.createTeacherSlot(payload),
    onSuccess: invalidateSchedule,
  })

  const updateSlot = useMutation({
    mutationFn: ({ payload, slotId }: UpdateSlotInput) =>
      backendService.schedule.updateTeacherSlot(slotId, payload),
    onSuccess: invalidateSchedule,
  })

  const announceSlot = useMutation({
    mutationFn: (slotId: BackendId) => backendService.schedule.announceTeacherSlot(slotId),
    onSuccess: invalidateSchedule,
  })

  const unannounceSlot = useMutation({
    mutationFn: (slotId: BackendId) => backendService.schedule.unannounceTeacherSlot(slotId),
    onSuccess: invalidateSchedule,
  })

  return { announceSlot, createSlot, unannounceSlot, updateSlot }
}

export function useTeacherBookingActions() {
  const queryClient = useQueryClient()

  const updateBooking = (bookingId: BackendId) => {
    void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
    void queryClient.invalidateQueries({ queryKey: scheduleKeys.teacherBooking(bookingId) })
  }

  const completeBooking = useMutation({
    mutationFn: (bookingId: BackendId) => backendService.schedule.completeTeacherBooking(bookingId),
    onSuccess: (booking) => updateBooking(booking.id),
  })

  const markNoShow = useMutation({
    mutationFn: (bookingId: BackendId) =>
      backendService.schedule.markTeacherBookingNoShow(bookingId),
    onSuccess: (booking) => updateBooking(booking.id),
  })

  return { completeBooking, markNoShow }
}

export function useAdminScheduleBookings(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.adminBookings(filters),
    queryFn: () => backendService.schedule.listAdminBookings(filters),
  })
}

export function useAdminScheduleBooking(bookingId?: BackendId) {
  return useQuery({
    enabled: Boolean(bookingId),
    queryKey: bookingId ? scheduleKeys.adminBooking(bookingId) : scheduleKeys.adminBooking(0),
    queryFn: () => backendService.schedule.getAdminBooking(bookingId as BackendId),
  })
}

export function useAdminScheduleProviderFailures(filters?: PageRequest) {
  return useQuery({
    queryKey: scheduleKeys.adminProviderFailures(filters),
    queryFn: () => backendService.schedule.listAdminProviderFailures(filters),
  })
}

export function liveSpeakingPaymentReturnUrl() {
  return currentBrowserUrl()
}
