package com.mockexams.backend.service;

import com.mockexams.backend.dto.SpeakingFeedbackRDO;
import com.mockexams.backend.dto.SpeakingSubmissionCDO;
import com.mockexams.backend.dto.SpeakingSubmissionRDO;
import com.mockexams.backend.users.entity.User;

public interface SpeakingSubmissionService {

	SubmissionResult submit(User student, SpeakingSubmissionCDO request);

	SpeakingSubmissionRDO get(User student, Long submissionId);

	SpeakingSubmissionRDO startTranscription(User student, Long submissionId);

	SpeakingFeedbackRDO getFeedback(User student, Long submissionId);

	SpeakingFeedbackRDO startFeedback(User student, Long submissionId);

	SpeakingFeedbackRDO retryFeedback(User student, Long submissionId);

	record SubmissionResult(SpeakingSubmissionRDO submission, boolean created) {
	}
}
