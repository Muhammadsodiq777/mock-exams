package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.TeacherListeningSegmentCDO;
import com.mockexams.backend.dto.TeacherListeningSegmentReorderUDO;
import com.mockexams.backend.dto.TeacherListeningSegmentUDO;
import com.mockexams.backend.dto.TeacherQuestionCDO;
import com.mockexams.backend.dto.TeacherQuestionUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphCDO;
import com.mockexams.backend.dto.TeacherReadingParagraphReorderUDO;
import com.mockexams.backend.dto.TeacherReadingParagraphUDO;
import com.mockexams.backend.dto.TeacherTestCDO;
import com.mockexams.backend.dto.TeacherTestRDO;
import com.mockexams.backend.dto.TeacherTestSectionCDO;
import com.mockexams.backend.dto.TeacherTestSectionReorderUDO;
import com.mockexams.backend.dto.TeacherTestSectionUDO;
import com.mockexams.backend.dto.TeacherTestSummaryRDO;
import com.mockexams.backend.dto.TeacherTestUDO;
import com.mockexams.backend.users.entity.User;

public interface TeacherTestBuilderService {

	Page<TeacherTestSummaryRDO> listOwnedTests(User teacher, String visibility, String search, Pageable pageable);

	TeacherTestRDO createDraft(User teacher, TeacherTestCDO command);

	TeacherTestRDO updateTest(User teacher, Long testId, TeacherTestUDO command);

	TeacherTestRDO addSection(User teacher, Long testId, TeacherTestSectionCDO command);

	TeacherTestRDO updateSection(User teacher, Long testId, Long sectionId, TeacherTestSectionUDO command);

	TeacherTestRDO reorderSections(User teacher, Long testId, TeacherTestSectionReorderUDO command);

	TeacherTestRDO deleteSection(User teacher, Long testId, Long sectionId);

	TeacherTestRDO addParagraph(User teacher, Long testId, Long sectionId, TeacherReadingParagraphCDO command);

	TeacherTestRDO updateParagraph(
			User teacher,
			Long testId,
			Long sectionId,
			Long paragraphId,
			TeacherReadingParagraphUDO command);

	TeacherTestRDO reorderParagraphs(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherReadingParagraphReorderUDO command);

	TeacherTestRDO deleteParagraph(User teacher, Long testId, Long sectionId, Long paragraphId);

	TeacherTestRDO addListeningSegment(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherListeningSegmentCDO command);

	TeacherTestRDO updateListeningSegment(
			User teacher,
			Long testId,
			Long sectionId,
			Long segmentId,
			TeacherListeningSegmentUDO command);

	TeacherTestRDO reorderListeningSegments(
			User teacher,
			Long testId,
			Long sectionId,
			TeacherListeningSegmentReorderUDO command);

	TeacherTestRDO deleteListeningSegment(User teacher, Long testId, Long sectionId, Long segmentId);

	TeacherTestRDO addQuestion(User teacher, Long testId, Long sectionId, TeacherQuestionCDO command);

	TeacherTestRDO updateQuestion(User teacher, Long testId, Long sectionId, Long questionId, TeacherQuestionUDO command);

	TeacherTestRDO deleteQuestion(User teacher, Long testId, Long sectionId, Long questionId);

	TeacherTestRDO preview(User teacher, Long testId);

	TeacherTestRDO submit(User teacher, Long testId);

	TeacherTestRDO archive(User teacher, Long testId);
}
