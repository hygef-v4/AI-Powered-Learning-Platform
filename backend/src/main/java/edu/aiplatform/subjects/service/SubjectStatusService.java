package edu.aiplatform.subjects.service;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC19: khóa dòng môn → U05 lưu trữ mọi lớp chưa lưu trữ qua {@link ISubjectClasses} cùng transaction → CAS → audit.
 * U05 khóa cùng dòng môn qua {@code ISubjectMutationGuard} trước mọi thao tác ghi lớp nên hai bên chạy lần lượt.
 */
@Service
public class SubjectStatusService {
    private final ISubjectRepository repository;
    private final SubjectAdminAccess access;
    private final ObjectProvider<ISubjectClasses> classes;
    private final SubjectAuditRecorder audit;
    private final Clock clock;

    public SubjectStatusService(ISubjectRepository repository, IAuthorization authorization,
                                ObjectProvider<ISubjectClasses> classes, SubjectAuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.access = new SubjectAdminAccess(authorization, audit, clock);
        this.classes = classes;
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
            ISubjectClasses provider = classes.getIfAvailable();
            if (provider == null) {
                throw classesUnavailable();
            }
            // BR-U04-04: lưu trữ môn thì lưu trữ luôn mọi lớp của môn; U05 lỗi thì cả thao tác rollback.
            try {
                provider.archiveClassesOfSubject(subjectId, actor);
            } catch (RuntimeException failure) {
                throw classesUnavailable();
            }
            budget.remaining();
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
