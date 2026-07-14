package com.mockexams.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

	Optional<Review> findByStudent_IdAndTest_Id(Long studentId, Long testId);

	Optional<Review> findByIdAndStudent_Id(Long id, Long studentId);

	@Override
	@EntityGraph(attributePaths = { "student", "test", "test.teacherProfile" })
	Page<Review> findAll(Specification<Review> specification, Pageable pageable);

	@Query("""
			select coalesce(avg(review.rating), 0) as average, count(review.id) as reviewCount
			from Review review
			where review.hiddenAt is null
			  and review.test.teacherProfile.id = :teacherProfileId
			  and review.test.deletedAt is null
			""")
	TeacherRatingAggregate aggregateTeacherRating(@Param("teacherProfileId") Long teacherProfileId);

	interface TeacherRatingAggregate {

		Double getAverage();

		Long getReviewCount();
	}
}
