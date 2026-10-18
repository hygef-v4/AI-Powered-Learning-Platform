package edu.aiplatform.subjects.application;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectArchiveCheck;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import java.util.List;
import org.springframework.http.HttpStatus;

/** B11 sẽ ánh xạ count/blockers thành ProblemDetail extensions sau khi service đã kiểm Admin. */
public class SubjectArchiveRejectedException extends ApiException {
    private final ISubjectArchiveCheck.Result check;

    public SubjectArchiveRejectedException(ISubjectArchiveCheck.Result check) {
        super(HttpStatus.CONFLICT, "SUBJECT_HAS_UNARCHIVED_CLASSES", "Môn còn lớp chưa lưu trữ");
        this.check = check;
    }

    public int unarchivedCount() { return check.unarchivedCount(); }
    public List<SubjectClassSummary> blockingClasses() { return check.blockingClasses(); }
}
