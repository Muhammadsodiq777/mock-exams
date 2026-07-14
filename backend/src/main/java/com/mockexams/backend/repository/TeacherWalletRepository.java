package com.mockexams.backend.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.TeacherWallet;

public interface TeacherWalletRepository extends JpaRepository<TeacherWallet, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	Optional<TeacherWallet> findByTeacherProfile_Id(Long teacherProfileId);

	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	Optional<TeacherWallet> findByTeacherProfile_User_Id(Long userId);

	/**
	 * Read-only lookup for admin summary views. Does not take the pessimistic write lock that
	 * {@link #findByTeacherProfile_Id(Long)} uses for wallet mutation flows, so displaying a balance
	 * does not block concurrent wallet writes.
	 */
	@EntityGraph(attributePaths = { "teacherProfile", "teacherProfile.user" })
	@Query("select wallet from TeacherWallet wallet where wallet.teacherProfile.id = :teacherProfileId")
	Optional<TeacherWallet> findForReadByTeacherProfileId(@Param("teacherProfileId") Long teacherProfileId);
}
