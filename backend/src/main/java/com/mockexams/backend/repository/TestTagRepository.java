package com.mockexams.backend.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.TestTag;

public interface TestTagRepository extends JpaRepository<TestTag, Long> {

	List<TestTag> findAllBy(Sort sort);
}
