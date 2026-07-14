package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.QuestionOption;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

	List<QuestionOption> findByQuestionIdOrderByOptionOrderAsc(Long questionId);

	List<QuestionOption> findByQuestionIdInOrderByQuestionIdAscOptionOrderAscIdAsc(List<Long> questionIds);

	Optional<QuestionOption> findByIdAndQuestionId(Long id, Long questionId);

	void deleteByQuestionId(Long questionId);
}
