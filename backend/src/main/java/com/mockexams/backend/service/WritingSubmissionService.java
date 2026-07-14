package com.mockexams.backend.service;

import com.mockexams.backend.dto.WritingFeedbackRDO;
import com.mockexams.backend.dto.WritingSubmissionCDO;
import com.mockexams.backend.dto.WritingSubmissionRDO;
import com.mockexams.backend.users.entity.User;

public interface WritingSubmissionService {

	WritingSubmissionRDO submit(User student, WritingSubmissionCDO command);

	WritingSubmissionRDO getSubmission(User student, Long submissionId);

	WritingFeedbackRDO getFeedback(User student, Long submissionId);

	WritingFeedbackRDO retryFeedback(User student, Long submissionId);
}
