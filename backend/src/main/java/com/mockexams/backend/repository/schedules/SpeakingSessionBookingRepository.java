package com.mockexams.backend.repository.schedules;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.schedules.SpeakingSessionBooking;

public interface SpeakingSessionBookingRepository extends JpaRepository<SpeakingSessionBooking, Long> {

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Page<SpeakingSessionBooking> findByTeacherProfile_Id(Long teacherProfileId, Pageable pageable);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	@Query("""
			select booking
			from SpeakingSessionBooking booking
			join booking.slot slot
			where booking.teacherProfile.id = :teacherProfileId
			  and (:status is null or booking.status = :status)
			  and (:from is null or slot.startsAt >= :from)
			  and (:to is null or slot.startsAt <= :to)
			""")
	Page<SpeakingSessionBooking> findTeacherBookings(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("status") String status,
			@Param("from") java.time.OffsetDateTime from,
			@Param("to") java.time.OffsetDateTime to,
			Pageable pageable);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Page<SpeakingSessionBooking> findByStudent_Id(Long studentId, Pageable pageable);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	@Query("""
			select booking
			from SpeakingSessionBooking booking
			join booking.slot slot
			where booking.student.id = :studentId
			  and (:status is null or booking.status = :status)
			  and (:from is null or slot.startsAt >= :from)
			  and (:to is null or slot.startsAt <= :to)
			""")
	Page<SpeakingSessionBooking> findStudentBookings(
			@Param("studentId") Long studentId,
			@Param("status") String status,
			@Param("from") java.time.OffsetDateTime from,
			@Param("to") java.time.OffsetDateTime to,
			Pageable pageable);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Optional<SpeakingSessionBooking> findByIdAndTeacherProfile_Id(Long id, Long teacherProfileId);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Optional<SpeakingSessionBooking> findByIdAndStudent_Id(Long id, Long studentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	@Query("select booking from SpeakingSessionBooking booking where booking.id = :id")
	Optional<SpeakingSessionBooking> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	@Query("select booking from SpeakingSessionBooking booking where booking.id = :id and booking.student.id = :studentId")
	Optional<SpeakingSessionBooking> findByIdAndStudentIdForUpdate(@Param("id") Long id, @Param("studentId") Long studentId);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Optional<SpeakingSessionBooking> findByPayment_Id(Long paymentId);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	Page<SpeakingSessionBooking> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "slot", "teacherProfile", "teacherProfile.user", "student", "payment" })
	@Query("""
			select booking
			from SpeakingSessionBooking booking
			join booking.slot slot
			where (:status is null or booking.status = :status)
			  and (:teacherProfileId is null or booking.teacherProfile.id = :teacherProfileId)
			  and (:studentUserId is null or booking.student.id = :studentUserId)
			  and (:from is null or slot.startsAt >= :from)
			  and (:to is null or slot.startsAt <= :to)
			""")
	Page<SpeakingSessionBooking> findAdminBookings(
			@Param("status") String status,
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("studentUserId") Long studentUserId,
			@Param("from") java.time.OffsetDateTime from,
			@Param("to") java.time.OffsetDateTime to,
			Pageable pageable);

	@Query("""
			select count(booking) > 0
			from SpeakingSessionBooking booking
			join booking.slot slot
			where booking.student.id = :studentId
			  and booking.status in ('reserved', 'booked')
			  and slot.startsAt < :endsAt
			  and slot.endsAt > :startsAt
			""")
	boolean existsActiveStudentOverlap(
			@Param("studentId") Long studentId,
			@Param("startsAt") java.time.OffsetDateTime startsAt,
			@Param("endsAt") java.time.OffsetDateTime endsAt);
}
