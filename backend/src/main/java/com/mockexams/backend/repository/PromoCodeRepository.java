package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.PromoCode;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

	Optional<PromoCode> findByCodeNormalized(String codeNormalized);

	boolean existsByCodeNormalized(String codeNormalized);

	Optional<PromoCode> findByIdAndTeacherProfile_Id(Long id, Long teacherProfileId);

	Page<PromoCode> findByTeacherProfile_Id(Long teacherProfileId, Pageable pageable);

	Page<PromoCode> findByTeacherProfile_IdAndActive(Long teacherProfileId, boolean active, Pageable pageable);

	@Query(value = """
			select p from PromoCode p
			where (:teacherProfileId is null or p.teacherProfile.id = :teacherProfileId)
			and (:active is null or p.active = :active)
			""",
			countQuery = """
					select count(p) from PromoCode p
					where (:teacherProfileId is null or p.teacherProfile.id = :teacherProfileId)
					and (:active is null or p.active = :active)
					""")
	Page<PromoCode> adminList(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("active") Boolean active,
			Pageable pageable);

	@Modifying
	@Query("""
			update PromoCode p
			set p.usedCount = p.usedCount + 1, p.updatedAt = :now
			where p.id = :promoCodeId
			and (p.usageLimit is null or p.usedCount < p.usageLimit)
			""")
	int consumeUsage(@Param("promoCodeId") Long promoCodeId, @Param("now") OffsetDateTime now);

	@Modifying
	@Query("""
			update PromoCode p
			set p.usedCount = p.usedCount - 1, p.updatedAt = :now
			where p.id = :promoCodeId
			and p.usedCount > 0
			""")
	int releaseUsage(@Param("promoCodeId") Long promoCodeId, @Param("now") OffsetDateTime now);
}
