package com.mockexams.backend.users.repository;

import java.time.OffsetDateTime;
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
import com.mockexams.backend.users.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmailIgnoreCase(String email);

	@Query("""
			select new com.mockexams.backend.dto.AdminDashboardStatusCountRDO(user.status, count(user))
			from User user
			group by user.status
			""")
	List<AdminDashboardStatusCountRDO> countGroupedByStatus();

	@Query("""
			select count(user)
			from User user
			where (:from is null or user.createdAt >= :from)
			  and (:to is null or user.createdAt <= :to)
			""")
	long countRegisteredInWindow(OffsetDateTime from, OffsetDateTime to);

	List<User> findTop5ByOrderByCreatedAtDescIdDesc();

	@EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
	Optional<User> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
	Optional<User> findWithRolesById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<User> findForUpdateById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
	Optional<User> findForUpdateWithRolesById(Long id);

	/**
	 * Locks every currently active user holding the given role, ordered deterministically by id so
	 * concurrent last-admin-protection checks acquire row locks in the same global order and cannot
	 * deadlock against each other.
	 *
	 * <p>Callers that already know the target row is currently an active admin (e.g. via an
	 * unlocked pre-check) must obtain its lock through THIS query rather than locking it separately
	 * beforehand: the target is, by definition, a member of "all active admins" and will be included
	 * here, so this single ordered pass is the only lock acquisition needed for that case. Taking a
	 * single-row lock on the target first and only then calling this method would reintroduce a
	 * lock-order-inversion deadlock between two concurrent callers targeting different rows.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
	@Query("""
			select distinct user
			from User user
			join user.userRoles userRole
			join userRole.role role
			where role.code = :roleCode
			  and user.status = :status
			order by user.id asc
			""")
	List<User> findActiveByRoleForUpdate(@Param("roleCode") String roleCode, @Param("status") String status);

	@Query(value = """
			select user.id
			from User user
			where (:status is null or user.status = :status)
			  and (:email is null or lower(user.email) like :email)
			  and (:teacherStatus is null or exists (
			        select 1
			        from TeacherProfile profile
			        where profile.user = user
			          and profile.status = :teacherStatus
			      ))
			  and (:role is null or exists (
			        select 1
			        from UserRole userRole
			        join userRole.role role
			        where userRole.user = user
			          and role.code = :role
			      ))
			order by user.createdAt desc, user.id desc
			""",
			countQuery = """
					select count(user.id)
					from User user
					where (:status is null or user.status = :status)
					  and (:email is null or lower(user.email) like :email)
					  and (:teacherStatus is null or exists (
					        select 1
					        from TeacherProfile profile
					        where profile.user = user
					          and profile.status = :teacherStatus
					      ))
					  and (:role is null or exists (
					        select 1
					        from UserRole userRole
					        join userRole.role role
					        where userRole.user = user
					          and role.code = :role
					      ))
					""")
	Page<Long> searchAdminUserIds(
			@Param("status") String status,
			@Param("role") String role,
			@Param("teacherStatus") String teacherStatus,
			@Param("email") String email,
			Pageable pageable);

	@EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
	List<User> findByIdIn(List<Long> ids);
}
