package com.mockexams.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.QuestionAnswer;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

	List<QuestionAnswer> findByQuestionIdOrderByAnswerOrderAsc(Long questionId);

	@Query("""
			select answer
			from QuestionAnswer answer
			join fetch answer.question question
			where question.id in :questionIds
			order by question.id asc, answer.answerOrder asc, answer.id asc
			""")
	List<QuestionAnswer> findByQuestionIdInOrderByQuestionIdAscAnswerOrderAscIdAsc(
			@Param("questionIds") List<Long> questionIds);

	void deleteByQuestionId(Long questionId);
}
