package edu.aiplatform.groupdocs.application;

import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.IAssignmentLifecycleListener;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Phản ứng của U12 trong transaction mở bài và ngưng giao của U07 (F7 bước 1, F13 bước 2): mở bài nhóm thì gửi job
 * tạo tài liệu (gửi sau commit); ngưng giao thì đánh dấu tài liệu để tự nộp ngay.
 */
@Component
public class GroupAssignmentLifecycle implements IAssignmentLifecycleListener {

    private final IAssignmentQuery assignments;
    private final GroupDocumentRepository documents;
    private final IJob jobs;

    public GroupAssignmentLifecycle(IAssignmentQuery assignments, GroupDocumentRepository documents, IJob jobs) {
        this.assignments = assignments;
        this.documents = documents;
        this.jobs = jobs;
    }

    @Override
    public void onOpened(UUID assignmentId) {
        Optional<AssignmentView> view = assignments.get(assignmentId);
        if (view.isPresent() && view.get().submissionMode() == SubmissionMode.GROUP) {
            jobs.enqueue(JobTypes.GROUP_DOC_CREATE, Map.of("assignmentId", assignmentId.toString()),
                    "group-doc-create:" + assignmentId);
        }
    }

    @Override
    public void onRetired(UUID assignmentId) {
        documents.markRetired(assignmentId);
    }
}
