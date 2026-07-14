package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	List<Question> findBySectionIdOrderByQuestionOrderAsc(Long sectionId);

	Optional<Question> findByIdAndSectionId(Long id, Long sectionId);

	@Query("""
			select question
			from Question question
			join question.section section
			where question.id = :questionId
			  and section.test.id = :testId
			""")
	Optional<Question> findByIdAndTestId(@Param("questionId") Long questionId, @Param("testId") Long testId);

	List<Question> findBySectionIdAndRelatedParagraphLabel(Long sectionId, String relatedParagraphLabel);

	boolean existsBySectionIdAndRelatedParagraphLabel(Long sectionId, String relatedParagraphLabel);

	@Query("""
			select question
			from Question question
			join fetch question.section section
			where section.id in :sectionIds
			order by section.sectionOrder asc, question.questionOrder asc, question.id asc
			""")
	List<Question> findBySectionIds(@Param("sectionIds") List<Long> sectionIds);

	@Query("select coalesce(max(question.questionOrder), 0) from Question question where question.section.id = :sectionId")
	Integer maxQuestionOrder(@Param("sectionId") Long sectionId);
}
