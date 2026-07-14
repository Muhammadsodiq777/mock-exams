package com.mockexams.backend.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.TestCategory;

public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {

	List<TestCategory> findByActiveTrue(Sort sort);
}
