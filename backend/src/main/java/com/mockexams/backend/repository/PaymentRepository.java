package com.mockexams.backend.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminDashboardStatusCountRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRowRDO;
import com.mockexams.backend.dto.AdminSalesReportRowRDO;
import com.mockexams.backend.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	@Query("""
			select new com.mockexams.backend.dto.AdminDashboardStatusCountRDO(payment.status, count(payment))
			from Payment payment
			where (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			group by payment.status
			""")
	List<AdminDashboardStatusCountRDO> countGroupedByStatusInWindow(OffsetDateTime from, OffsetDateTime to);

	@Query("""
			select count(payment)
			from Payment payment
			where payment.failureCode = :failureCode
			  and (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			""")
	long countByFailureCodeInWindow(String failureCode, OffsetDateTime from, OffsetDateTime to);

	@Query("""
			select coalesce(sum(payment.amount), 0)
			from Payment payment
			where payment.status = :status
			  and (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			""")
	BigDecimal sumAmountByStatusInWindow(String status, OffsetDateTime from, OffsetDateTime to);

	long countByStatus(String status);

	long countByFailureCode(String failureCode);

	List<Payment> findTop5ByStatusOrderByCreatedAtDescIdDesc(String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	Optional<Payment> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	Optional<Payment> findByIdAndStudent_Id(Long id, Long studentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	@Query("select payment from Payment payment where payment.id = :id and payment.student.id = :studentId")
	Optional<Payment> findByIdAndStudentIdForUpdate(@Param("id") Long id, @Param("studentId") Long studentId);

	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	List<Payment> findByStudent_IdOrderByCreatedAtDescIdDesc(Long studentId);

	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	Optional<Payment> findFirstByTargetLiveSpeakingBookingIdAndStatusInOrderByCreatedAtDescIdDesc(
			Long bookingId,
			Collection<String> statuses);

	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	Page<Payment> findByStatus(String status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "student", "targetListing", "targetListing.teacherProfile", "targetListing.test" })
	Page<Payment> findAll(Pageable pageable);

	@Query("""
			select new com.mockexams.backend.dto.AdminSalesReportRowRDO(
				cast(payment.createdAt as LocalDate),
				teacherProfile.id,
				listing.id,
				payment.targetType,
				payment.provider,
				payment.status,
				payment.currency,
				count(distinct payment),
				coalesce(sum(payment.amount), 0),
				coalesce(sum(case when payment.status = 'paid' then payment.amount else 0 end), 0),
				coalesce(sum(case when payment.status = 'failed' then payment.amount else 0 end), 0),
				coalesce(sum(case when payment.status = 'refunded' then payment.amount else 0 end), 0),
				cast(0 as long)
			)
			from Payment payment
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
			order by cast(payment.createdAt as LocalDate) asc, teacherProfile.id asc, listing.id asc,
				payment.provider asc, payment.status asc
			""")
	List<AdminSalesReportRowRDO> salesReportRows(
			Long teacherProfileId,
			Long listingId,
			String provider,
			String paymentStatus,
			OffsetDateTime from,
			OffsetDateTime to);

	@Query("""
			select new com.mockexams.backend.dto.AdminProviderFailureReportRowRDO(
				cast(payment.createdAt as LocalDate),
				'payment',
				payment.provider,
				payment.status,
				payment.failureCode,
				null,
				payment.targetType,
				payment.currency,
				count(payment),
				coalesce(sum(payment.amount), 0),
				0
			)
			from Payment payment
			where (payment.status = 'failed' or payment.failureCode is not null)
			  and (:provider is null or payment.provider = :provider)
			  and (:paymentStatus is null or payment.status = :paymentStatus)
			  and (:from is null or payment.createdAt >= :from)
			  and (:to is null or payment.createdAt <= :to)
			group by cast(payment.createdAt as LocalDate), payment.provider, payment.status, payment.failureCode,
				payment.targetType, payment.currency
			order by cast(payment.createdAt as LocalDate) asc, payment.provider asc, payment.failureCode asc,
				payment.status asc
			""")
	List<AdminProviderFailureReportRowRDO> paymentFailureReportRows(
			String provider,
			String paymentStatus,
			OffsetDateTime from,
			OffsetDateTime to);
}
