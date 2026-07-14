package com.mockexams.backend.service;

import com.mockexams.backend.dto.StudentAnswerRDO;
import com.mockexams.backend.dto.StudentAnswerUDO;
import com.mockexams.backend.dto.StudentTestAttemptRDO;
import com.mockexams.backend.users.entity.User;

public interface StudentTestAttemptService {

	StudentTestAttemptRDO startAttempt(User student, Long testId);

	StudentAnswerRDO saveAnswer(User student, Long attemptId, StudentAnswerUDO request);

	StudentTestAttemptRDO submitAttempt(User student, Long attemptId);
}
