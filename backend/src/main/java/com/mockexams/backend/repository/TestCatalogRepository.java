package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminDashboardStatusCountRDO;
import com.mockexams.backend.entity.CatalogTest;

public interface TestCatalogRepository extends JpaRepository<CatalogTest, Long>, JpaSpecificationExecutor<CatalogTest> {

	@Query("""
			select new com.mockexams.backend.dto.AdminDashboardStatusCountRDO(test.visibility, count(test))
			from CatalogTest test
			where test.deletedAt is null
			group by test.visibility
			""")
	List<AdminDashboardStatusCountRDO> countGroupedByVisibility();

	long countByVisibilityAndDeletedAtIsNull(String visibility);

	List<CatalogTest> findTop5ByVisibilityAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(String visibility);

	@EntityGraph(attributePaths = { "teacherProfile", "categories", "tags" })
	@Query("""
			select distinct test
			from CatalogTest test
			where test.id in :testIds
			""")
	List<CatalogTest> findReadModelsByIdIn(@Param("testIds") List<Long> testIds);

	@EntityGraph(attributePaths = { "teacherProfile", "categories", "tags" })
	@Query("""
			select distinct test
			from CatalogTest test
			left join test.teacherProfile teacherProfile
			where test.id = :testId
			  and test.visibility = 'published'
			  and test.deletedAt is null
			  and (
			    teacherProfile.status = 'approved'
			    or (
			      teacherProfile is null
			      and exists (
			        select 1
			        from UserRole userRole
			        join userRole.role role
			        where userRole.id.userId = test.createdByUserId
			          and role.code = 'admin'
			      )
			    )
			  )
			""")
	Optional<CatalogTest> findVisibleDetailById(@Param("testId") Long testId);

	@EntityGraph(attributePaths = { "teacherProfile" })
	Optional<CatalogTest> findByIdAndTeacherProfileIdAndDeletedAtIsNull(Long id, Long teacherProfileId);

	@EntityGraph(attributePaths = { "teacherProfile" })
	@Query(value = """
			select test
			from CatalogTest test
			join test.teacherProfile teacherProfile
			where teacherProfile.id = :teacherProfileId
			  and test.deletedAt is null
			  and (:visibility is null or test.visibility = :visibility)
			  and (
			    :search is null
			    or lower(test.title) like :search
			    or lower(coalesce(test.description, '')) like :search
			  )
			""",
			countQuery = """
					select count(test)
					from CatalogTest test
					join test.teacherProfile teacherProfile
					where teacherProfile.id = :teacherProfileId
					  and test.deletedAt is null
					  and (:visibility is null or test.visibility = :visibility)
					  and (
					    :search is null
					    or lower(test.title) like :search
					    or lower(coalesce(test.description, '')) like :search
					  )
					""")
	Page<CatalogTest> findTeacherOwnedTests(
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("visibility") String visibility,
			@Param("search") String search,
			Pageable pageable);

	@EntityGraph(attributePaths = { "teacherProfile" })
	@Query("""
			select distinct test
			from CatalogTest test
			left join test.teacherProfile teacherProfile
			where test.type = :testType
			  and test.visibility = 'published'
			  and test.deletedAt is null
			  and test.free = true
			  and (
			    teacherProfile.status = 'approved'
			    or (
			      teacherProfile is null
			      and exists (
			        select 1
			        from UserRole userRole
			        join userRole.role role
			        where userRole.id.userId = test.createdByUserId
			          and role.code = 'admin'
			      )
			    )
			  )
			order by test.publishedAt desc, test.id desc
			""")
	List<CatalogTest> findRecommendedFreeVisibleByType(@Param("testType") String testType, Pageable pageable);

	@EntityGraph(attributePaths = { "teacherProfile" })
	@Query("""
			select distinct test
			from CatalogTest test
			left join test.teacherProfile teacherProfile
			where test.id = :testId
			  and test.visibility = 'published'
			  and test.deletedAt is null
			  and test.free = true
			  and (
			    teacherProfile.status = 'approved'
			    or (
			      teacherProfile is null
			      and exists (
			        select 1
			        from UserRole userRole
			        join userRole.role role
			        where userRole.id.userId = test.createdByUserId
			          and role.code = 'admin'
			      )
			    )
			  )
			""")
	Optional<CatalogTest> findVisibleFreeById(@Param("testId") Long testId);

	@EntityGraph(attributePaths = { "teacherProfile" })
	@Query(value = """
			select test from CatalogTest test
			left join test.teacherProfile teacherProfile
			where test.visibility = 'submitted'
			  and test.deletedAt is null
			  and (:type is null or test.type = :type)
			  and (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
			  and (:difficulty is null or test.difficulty = :difficulty)
			  and (:submittedFrom is null or test.updatedAt >= :submittedFrom)
			  and (:submittedTo is null or test.updatedAt <= :submittedTo)
			  and (:search is null or lower(test.title) like :search)
			""",
			countQuery = """
					select count(test) from CatalogTest test
					left join test.teacherProfile teacherProfile
					where test.visibility = 'submitted'
					  and test.deletedAt is null
					  and (:type is null or test.type = :type)
					  and (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
					  and (:difficulty is null or test.difficulty = :difficulty)
					  and (:submittedFrom is null or test.updatedAt >= :submittedFrom)
					  and (:submittedTo is null or test.updatedAt <= :submittedTo)
					  and (:search is null or lower(test.title) like :search)
					""")
	Page<CatalogTest> adminModerationQueue(
			@Param("type") String type,
			@Param("teacherProfileId") Long teacherProfileId,
			@Param("difficulty") String difficulty,
			@Param("submittedFrom") OffsetDateTime submittedFrom,
			@Param("submittedTo") OffsetDateTime submittedTo,
			@Param("search") String search,
			Pageable pageable);

	Optional<CatalogTest> findTopByVisibilityAndDeletedAtIsNullOrderByUpdatedAtAsc(String visibility);

	long countByTeacherProfile_IdAndDeletedAtIsNull(Long teacherProfileId);

	long countByTeacherProfile_IdAndVisibilityAndDeletedAtIsNull(Long teacherProfileId, String visibility);

	List<CatalogTest> findTop5ByTeacherProfile_IdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(Long teacherProfileId);
}
