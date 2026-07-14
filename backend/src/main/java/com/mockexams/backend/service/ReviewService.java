package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.AdminReviewRDO;
import com.mockexams.backend.dto.PublicReviewRDO;
import com.mockexams.backend.dto.ReviewCDO;
import com.mockexams.backend.dto.ReviewUDO;
import com.mockexams.backend.dto.StudentReviewRDO;
import com.mockexams.backend.dto.TeacherReviewRDO;
import com.mockexams.backend.dto.TeacherReviewSummaryRDO;
import com.mockexams.backend.users.entity.User;

public interface ReviewService {

	StudentReviewRDO createTestReview(User student, Long testId, ReviewCDO command);

	StudentReviewRDO updateOwnReview(User student, Long reviewId, ReviewUDO command);

	Page<PublicReviewRDO> publicTestReviews(Long testId, ReviewSearchCriteria criteria, Pageable pageable);

	Page<PublicReviewRDO> publicTeacherReviews(Long teacherProfileId, ReviewSearchCriteria criteria, Pageable pageable);

	Page<TeacherReviewRDO> teacherContentReviews(User teacher, ReviewSearchCriteria criteria, Pageable pageable);

	TeacherReviewSummaryRDO teacherReviewSummary(User teacher);

	Page<AdminReviewRDO> adminReviews(AdminReviewSearchCriteria criteria, Pageable pageable);

	AdminReviewRDO adminHide(User admin, Long reviewId);

	AdminReviewRDO adminUnhide(User admin, Long reviewId);

	void adminDelete(User admin, Long reviewId);
}
