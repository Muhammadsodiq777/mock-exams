package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.TestSection;

public interface TestSectionRepository extends JpaRepository<TestSection, Long> {

	List<TestSection> findByTestIdOrderBySectionOrderAsc(Long testId);

	Optional<TestSection> findByIdAndTestId(Long id, Long testId);

	long countByTestId(Long testId);

	@Query("select coalesce(max(section.sectionOrder), 0) from TestSection section where section.test.id = :testId")
	Integer maxSectionOrder(@Param("testId") Long testId);
}
