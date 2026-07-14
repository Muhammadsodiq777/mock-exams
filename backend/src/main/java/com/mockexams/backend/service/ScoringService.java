package com.mockexams.backend.service;

import com.mockexams.backend.dto.ResultDetailRDO;
import com.mockexams.backend.users.entity.User;

public interface ScoringService {

	ResultDetailRDO getOrCreateResultForAttempt(User student, Long attemptId);

	ResultDetailRDO getResult(User student, Long resultId);
}
