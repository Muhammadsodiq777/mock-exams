package com.mockexams.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.TestCardRDO;
import com.mockexams.backend.dto.TestCategoryRDO;
import com.mockexams.backend.dto.TestDetailRDO;
import com.mockexams.backend.dto.TestTagRDO;

public interface TestCatalogService {

	Page<TestCardRDO> listTests(TestCatalogSearchCriteria criteria, Pageable pageable);

	TestDetailRDO testDetail(Long testId);

	List<TestCategoryRDO> listCategories();

	List<TestTagRDO> listTags();
}
