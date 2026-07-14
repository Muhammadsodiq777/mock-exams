package com.mockexams.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.StudentAnswer;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

	@EntityGraph(attributePaths = { "attempt", "question", "selectedOption" })
	Optional<StudentAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

	@Query("""
			select answer
			from StudentAnswer answer
			join fetch answer.question question
			left join fetch answer.selectedOption selectedOption
			where answer.attempt.id = :attemptId
			order by question.questionOrder asc, answer.id asc
			""")
	List<StudentAnswer> findByAttemptIdForResult(@Param("attemptId") Long attemptId);
}
