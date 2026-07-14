package com.mockexams.backend.service;

import java.util.List;

import com.mockexams.backend.dto.StudyPlanCDO;
import com.mockexams.backend.dto.StudyPlanRDO;
import com.mockexams.backend.dto.StudyPlanTaskRDO;
import com.mockexams.backend.users.entity.User;

public interface StudyPlanService {

	StudyPlanRDO create(User student, StudyPlanCDO command);

	StudyPlanRDO get(User student, Long planId);

	List<StudyPlanRDO> activePlans(User student);

	List<StudyPlanRDO> history(User student);

	StudyPlanTaskRDO completeTask(User student, Long planId, Long taskId);

	StudyPlanTaskRDO uncompleteTask(User student, Long planId, Long taskId);

	StudyPlanRDO regenerate(User student, Long planId);
}
