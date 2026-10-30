package edu.aiplatform.subjects.service;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectMutationGuard;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Duration;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectMutationGuard implements ISubjectMutationGuard {
    private final ISubjectRepository repository;

    public SubjectMutationGuard(ISubjectRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SubjectRef lockActive(UUID subjectId, Duration timeout) {
        if (subjectId == null) {
            throw ApiException.badRequest("SUBJECT_ID_REQUIRED", "Cần mã định danh môn học");
        }
        SubjectLifecycleBudget budget = new SubjectLifecycleBudget(timeout);
        Subject subject;
        try {
            subject = repository.findByIdForUpdate(subjectId, budget.remaining())
                    .orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"));
        } catch (DataAccessException failure) {
            throw ApiException.unavailable("SUBJECT_LIFECYCLE_UNAVAILABLE", "Chưa khóa được môn, vui lòng thử lại");
        }
        budget.remaining();
        if (subject.status() != SubjectStatus.ACTIVE) {
            throw ApiException.conflict("SUBJECT_ARCHIVED", "Môn đã lưu trữ, không thể tạo hoặc mở lớp");
        }
        return new SubjectRef(subject.id(), subject.code().value(), subject.name(), subject.status(), subject.managerId());
    }
}
