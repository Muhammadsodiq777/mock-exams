package com.mockexams.backend.teachers.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminDashboardStatusCountRDO;
import com.mockexams.backend.teachers.entity.TeacherProfile;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

	@Query("""
			select new com.mockexams.backend.dto.AdminDashboardStatusCountRDO(profile.status, count(profile))
			from TeacherProfile profile
			group by profile.status
			""")
	List<AdminDashboardStatusCountRDO> countGroupedByStatus();

	long countByStatus(String status);

	Optional<TeacherProfile> findByUserId(Long userId);

	Optional<TeacherProfile> findByIdAndStatus(Long id, String status);

	Page<TeacherProfile> findByStatus(String status, Pageable pageable);

	@Query("""
			select profile
			from TeacherProfile profile
			join fetch profile.user user
			where user.id in :userIds
			""")
	List<TeacherProfile> findByUserIdsWithUser(@Param("userIds") List<Long> userIds);
}
