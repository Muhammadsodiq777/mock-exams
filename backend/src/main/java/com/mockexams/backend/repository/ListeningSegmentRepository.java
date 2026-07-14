package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.ListeningSegment;

public interface ListeningSegmentRepository extends JpaRepository<ListeningSegment, Long> {

	List<ListeningSegment> findBySectionIdOrderBySegmentOrderAsc(Long sectionId);

	Optional<ListeningSegment> findByIdAndSectionId(Long id, Long sectionId);

	boolean existsByIdAndSectionId(Long id, Long sectionId);

	@Query("""
			select segment
			from ListeningSegment segment
			where segment.section.id in :sectionIds
			order by segment.section.id asc, segment.segmentOrder asc, segment.id asc
			""")
	List<ListeningSegment> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);

	@Query("select coalesce(max(segment.segmentOrder), 0) from ListeningSegment segment where segment.section.id = :sectionId")
	Integer maxSegmentOrder(@Param("sectionId") Long sectionId);
}
