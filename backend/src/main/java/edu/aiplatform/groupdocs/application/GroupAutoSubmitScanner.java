package edu.aiplatform.groupdocs.application;

import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.SectionRules;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tự nộp mỗi phút (F13, BR-U12-43): tài liệu đang làm của bài đã quá hạn cuối 30 giây, hoặc bài đã ngưng giao.
 * Mỗi tài liệu một transaction; lỗi một tài liệu không chặn tài liệu khác, lượt sau thử lại.
 */
@Component
public class GroupAutoSubmitScanner implements IScheduledScanner {

    private static final Logger log = LoggerFactory.getLogger(GroupAutoSubmitScanner.class);

    private final GroupDocumentRepository documents;
    private final GroupSubmissionService submissions;
    private final IAssignmentQuery assignments;

    public GroupAutoSubmitScanner(GroupDocumentRepository documents, GroupSubmissionService submissions,
                                  IAssignmentQuery assignments) {
        this.documents = documents;
        this.submissions = submissions;
        this.assignments = assignments;
    }

    @Override
    public String name() {
        return "group-auto-submit";
    }

    @Override
    public void scan(Instant now) {
        List<UUID> overdue = new ArrayList<>();
        for (UUID assignmentId : documents.listInProgressAssignments()) {
            try {
                if (assignments.submissionWindow(assignmentId, now.minus(SectionRules.GRACE)) == SubmissionWindow.CLOSED) {
                    overdue.add(assignmentId);
                }
            } catch (RuntimeException e) {
                log.warn("Không đọc được hạn bài {}: {}", assignmentId, e.getClass().getName());
            }
        }
        for (UUID documentId : documents.listDueForAutoSubmit(overdue)) {
            try {
                submissions.autoSubmit(documentId);
            } catch (RuntimeException e) {
                log.warn("Tự nộp tài liệu {} lỗi, thử lại lượt sau: {}", documentId, e.getClass().getName());
            }
        }
    }
}
