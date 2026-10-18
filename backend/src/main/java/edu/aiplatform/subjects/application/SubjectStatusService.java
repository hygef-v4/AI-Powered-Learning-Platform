package edu.aiplatform.subjects.application;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectArchiveCheck;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** UC19: khóa môn → kiểm lớp cùng transaction → CAS → audit. Không gọi reader async B07. */
@Service
public class SubjectStatusService {
    private final ISubjectRepository repository;
    private final SubjectAdminAccess access;
    private final ObjectProvider<ISubjectArchiveCheck> checks;
    private final SubjectAuditRecorder audit;
    private final Clock clock;

    public SubjectStatusService(ISubjectRepository repository, IAuthorization authorization,
                                ObjectProvider<ISubjectArchiveCheck> checks, SubjectAuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.access = new SubjectAdminAccess(authorization, audit, clock);
        this.checks = checks;
        this.audit = audit;
        this.clock = clock;
    }

    public record ChangeStatus(SubjectStatus status, Long version) { }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Subject changeStatus(Actor actor, UUID subjectId, ChangeStatus command) {
        access.require(actor, Actions.SUBJECT_STATUS_CHANGE, ResourceRef.subject(subjectId));
        if (subjectId == null || command == null || command.status() == null) {
            throw ApiException.badRequest("SUBJECT_STATUS_INVALID", "Cần môn và trạng thái hợp lệ");
        }
        if (command.version() == null || command.version() < 1) {
            throw ApiException.badRequest("SUBJECT_VERSION_REQUIRED", "Cần version môn từ 1");
        }
        SubjectLifecycleBudget budget = new SubjectLifecycleBudget(Duration.ofSeconds(1));
        Subject before;
        try {
            before = repository.findByIdForUpdate(subjectId, budget.remaining())
                    .orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"));
        } catch (DataAccessException failure) {
            throw ApiException.unavailable("SUBJECT_LIFECYCLE_UNAVAILABLE", "Chưa khóa được môn, vui lòng thử lại");
        }
        before.requireVersion(command.version());
        if (before.status() == command.status()) {
            return before;
        }
        budget.remaining();
        if (command.status() == SubjectStatus.ARCHIVED) {
            ISubjectArchiveCheck checker = checks.getIfAvailable();
            if (checker == null) {
                throw classesUnavailable();
            }
            ISubjectArchiveCheck.Result result;
            try {
                result = checker.checkArchive(subjectId, budget.remaining());
            } catch (RuntimeException failure) {
                throw classesUnavailable();
            }
            budget.remaining();
            if (result == null) {
                throw classesUnavailable();
            }
            if (result.unarchivedCount() > 0) {
                throw new SubjectArchiveRejectedException(result);
            }
        }
        Subject after = command.status() == SubjectStatus.ARCHIVED
                ? before.archive(clock.instant()) : before.reopen(clock.instant());
        if (!repository.update(after, command.version())) {
            throw ApiException.conflict("SUBJECT_VERSION_CONFLICT", "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
        }
        audit.statusChanged(actor, before, after);
        if (before.managerId() != null && after.managerId() == null) {
            audit.managerChanged(actor, before, after);
        }
        return after;
    }

    private ApiException classesUnavailable() {
        return ApiException.unavailable("SUBJECT_CLASSES_UNAVAILABLE", "Chưa kiểm tra được lớp, vui lòng thử lại");
    }
}
