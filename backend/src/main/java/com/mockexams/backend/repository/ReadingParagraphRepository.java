package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.ReadingParagraph;

public interface ReadingParagraphRepository extends JpaRepository<ReadingParagraph, Long> {

	List<ReadingParagraph> findBySectionIdOrderByParagraphOrderAsc(Long sectionId);

	Optional<ReadingParagraph> findByIdAndSectionId(Long id, Long sectionId);

	boolean existsBySectionIdAndLabel(Long sectionId, String label);

	boolean existsBySectionIdAndLabelAndIdNot(Long sectionId, String label, Long id);

	@Query("""
			select paragraph
			from ReadingParagraph paragraph
			where paragraph.section.id in :sectionIds
			order by paragraph.section.id asc, paragraph.paragraphOrder asc, paragraph.id asc
			""")
	List<ReadingParagraph> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);

	@Query("""
			select coalesce(max(paragraph.paragraphOrder), 0)
			from ReadingParagraph paragraph
			where paragraph.section.id = :sectionId
			""")
	Integer maxParagraphOrder(@Param("sectionId") Long sectionId);
}
