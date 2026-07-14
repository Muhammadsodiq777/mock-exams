package com.mockexams.backend.repository.schedules;

import java.time.OffsetDateTime;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.schedules.SpeakingSessionSlot;

public interface SpeakingSessionSlotRepository extends JpaRepository<SpeakingSessionSlot, Long> {

	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	Page<SpeakingSessionSlot> findByTeacherProfile_Id(Long teacherProfileId, Pageable pageable);

	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	@Query("""
			select slot
			from SpeakingSessionSlot slot
			where slot.teacherProfile.id = :teacherProfileId
			  and (:status is null or slot.status = :status)
			  and (:from is null or slot.startsAt >= :from)
			  and (:to is null or slot.startsAt <= :to)
			""")
	Page<SpeakingSessionSlot> findTeacherSlots(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("status") String status,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to,
			Pageable pageable);

	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	Optional<SpeakingSessionSlot> findByIdAndTeacherProfile_Id(Long id, Long teacherProfileId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	@Query("select slot from SpeakingSessionSlot slot where slot.id = :id")
	Optional<SpeakingSessionSlot> findByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	@Query("""
			select slot
			from SpeakingSessionSlot slot
			join slot.teacherProfile profile
			where profile.status = 'approved'
			  and slot.status = 'announced'
			  and (:teacherProfileId is null or profile.id = :teacherProfileId)
			  and (:status is null or slot.status = :status)
			  and (:from is null or slot.startsAt >= :from)
			  and (:to is null or slot.startsAt <= :to)
			""")
	Page<SpeakingSessionSlot> findStudentVisibleSlots(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("status") String status,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to,
			Pageable pageable);

	@Query("""
			select count(slot) > 0
			from SpeakingSessionSlot slot
			where slot.teacherProfile.id = :teacherProfileId
			  and slot.id <> :slotId
			  and slot.status in ('announced', 'reserved', 'booked')
			  and slot.startsAt < :endsAt
			  and slot.endsAt > :startsAt
			""")
	boolean existsTeacherOverlap(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("slotId") Long slotId,
			@Param("startsAt") OffsetDateTime startsAt,
			@Param("endsAt") OffsetDateTime endsAt);
}
