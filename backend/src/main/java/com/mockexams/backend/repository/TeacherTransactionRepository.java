package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mockexams.backend.dto.AdminRevenueReportRowRDO;
import com.mockexams.backend.dto.TeacherWalletReportAggregateRDO;
import com.mockexams.backend.entity.TeacherTransaction;

public interface TeacherTransactionRepository extends JpaRepository<TeacherTransaction, Long> {

	@EntityGraph(attributePaths = { "wallet", "wallet.teacherProfile" })
	Optional<TeacherTransaction> findByPurchaseIdAndTransactionType(Long purchaseId, String transactionType);

	@EntityGraph(attributePaths = { "wallet", "wallet.teacherProfile" })
	Optional<TeacherTransaction> findBySourceTypeAndSourceReferenceAndTransactionType(
			String sourceType,
			String sourceReference,
			String transactionType);

	Page<TeacherTransaction> findByWallet_Id(Long walletId, Pageable pageable);

	@Query("""
			select new com.mockexams.backend.dto.TeacherWalletReportAggregateRDO(
				cast(transaction.createdAt as LocalDate),
				transaction.sourceType,
				count(transaction),
				coalesce(sum(transaction.grossAmount), 0),
				coalesce(sum(transaction.platformCommissionAmount), 0),
				coalesce(sum(transaction.netAmount), 0)
			)
			from TeacherTransaction transaction
			where (:walletId is null or transaction.wallet.id = :walletId)
			  and (:from is null or transaction.createdAt >= :from)
			  and (:to is null or transaction.createdAt <= :to)
			group by cast(transaction.createdAt as LocalDate), transaction.sourceType
			order by cast(transaction.createdAt as LocalDate) desc, transaction.sourceType asc
			""")
	List<TeacherWalletReportAggregateRDO> reportAggregatesByDateAndSource(
			Long walletId,
			OffsetDateTime from,
			OffsetDateTime to);

	@Query("""
			select new com.mockexams.backend.dto.AdminRevenueReportRowRDO(
				cast(transaction.createdAt as LocalDate),
				teacherProfile.id,
				listing.id,
				transaction.transactionType,
				transaction.sourceType,
				transaction.currency,
				count(transaction),
				coalesce(sum(transaction.grossAmount), 0),
				coalesce(sum(transaction.platformCommissionAmount), 0),
				coalesce(sum(transaction.netAmount), 0),
				coalesce(sum(case when transaction.transactionType = 'refund' then 1 else 0 end), 0)
			)
			from TeacherTransaction transaction
			join transaction.wallet wallet
			join wallet.teacherProfile teacherProfile
			left join Purchase purchase on purchase.id = transaction.purchaseId
			left join purchase.listing listing
			where (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
			  and (:listingId is null or listing.id = :listingId)
			  and (:from is null or transaction.createdAt >= :from)
			  and (:to is null or transaction.createdAt <= :to)
			group by cast(transaction.createdAt as LocalDate), teacherProfile.id, listing.id,
				transaction.transactionType, transaction.sourceType, transaction.currency
			order by cast(transaction.createdAt as LocalDate) asc, teacherProfile.id asc, listing.id asc,
				transaction.transactionType asc, transaction.sourceType asc
			""")
	List<AdminRevenueReportRowRDO> revenueReportRows(
			Long teacherProfileId,
			Long listingId,
			OffsetDateTime from,
			OffsetDateTime to);
}
