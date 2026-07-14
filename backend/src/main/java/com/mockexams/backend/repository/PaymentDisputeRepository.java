package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminSalesReportDisputeRowRDO;
import com.mockexams.backend.entity.PaymentDispute;

public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, Long> {

	@EntityGraph(attributePaths = { "payment", "payment.targetListing", "payment.targetListing.teacherProfile", "openedByUser" })
	Optional<PaymentDispute> findByIdAndOpenedByUser_Id(Long id, Long openedByUserId);

	@Override
	@EntityGraph(attributePaths = { "payment", "payment.targetListing", "payment.targetListing.teacherProfile", "openedByUser" })
	Optional<PaymentDispute> findById(Long id);

	@EntityGraph(attributePaths = { "payment", "payment.targetListing", "payment.targetListing.teacherProfile", "openedByUser" })
	Page<PaymentDispute> findByOpenedByUser_Id(Long openedByUserId, Pageable pageable);

	@EntityGraph(attributePaths = { "payment", "payment.targetListing", "payment.targetListing.teacherProfile", "openedByUser" })
	Page<PaymentDispute> findByOpenedByUser_IdAndStatus(Long openedByUserId, String status, Pageable pageable);

	boolean existsByPayment_IdAndStatusIn(Long paymentId, Collection<String> statuses);

	@Query(value = """
			select d from PaymentDispute d
			join fetch d.payment p
			join fetch d.openedByUser u
			left join fetch p.targetListing l
			left join fetch l.teacherProfile tp
			where (:status is null or d.status = :status)
			  and (:paymentStatus is null or p.status = :paymentStatus)
			  and (:from is null or d.createdAt >= :from)
			  and (:to is null or d.createdAt <= :to)
			  and (:teacherProfileId is null or tp.id = :teacherProfileId)
			  and (:studentUserId is null or u.id = :studentUserId)
			  and (:reason is null or lower(d.reason) like lower(concat('%', :reason, '%')))
			""",
			countQuery = """
					select count(d) from PaymentDispute d
					left join d.payment p
					left join p.targetListing l
					left join l.teacherProfile tp
					where (:status is null or d.status = :status)
					  and (:paymentStatus is null or p.status = :paymentStatus)
					  and (:from is null or d.createdAt >= :from)
					  and (:to is null or d.createdAt <= :to)
					  and (:teacherProfileId is null or tp.id = :teacherProfileId)
					  and (:studentUserId is null or d.openedByUser.id = :studentUserId)
					  and (:reason is null or lower(d.reason) like lower(concat('%', :reason, '%')))
					""")
	Page<PaymentDispute> adminQueue(
			@Param("status") String status,
			@Param("paymentStatus") String paymentStatus,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to,
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("studentUserId") Long studentUserId,
			@Param("reason") String reason,
			Pageable pageable);

	@Query("""
			select count(dispute)
			from PaymentDispute dispute
			join dispute.payment payment
			left join payment.targetListing listing
			left join listing.teacherProfile teacherProfile
			where (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
			  and (:listingId is null or listing.id = :listingId)
			  and (:provider is null or payment.provider = :provider)
			  and (:paymentStatus is null or payment.status = :paymentStatus)
			  and (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			""")
	long countForSalesReport(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("listingId") Long listingId,
			@Param("provider") String provider,
			@Param("paymentStatus") String paymentStatus,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select new com.mockexams.backend.dto.AdminSalesReportDisputeRowRDO(
				cast(payment.createdAt as LocalDate),
				teacherProfile.id,
				listing.id,
				payment.targetType,
				payment.provider,
				payment.status,
				payment.currency,
				count(dispute)
			)
			from PaymentDispute dispute
			join dispute.payment payment
			left join payment.targetListing listing
			left join listing.teacherProfile teacherProfile
			where (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
			  and (:listingId is null or listing.id = :listingId)
			  and (:provider is null or payment.provider = :provider)
			  and (:paymentStatus is null or payment.status = :paymentStatus)
			  and (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			group by cast(payment.createdAt as LocalDate), teacherProfile.id, listing.id, payment.targetType,
				payment.provider, payment.status, payment.currency
			""")
	List<AdminSalesReportDisputeRowRDO> salesReportDisputeRows(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("listingId") Long listingId,
			@Param("provider") String provider,
			@Param("paymentStatus") String paymentStatus,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);
}
