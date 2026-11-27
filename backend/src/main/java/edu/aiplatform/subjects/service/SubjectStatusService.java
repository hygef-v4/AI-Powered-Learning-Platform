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
    private final ISubjectRepository subjectRepository;
    private final SubjectAdminAccess subjectAdminAccess;
    private final ObjectProvider<ISubjectClasses> subjectClassesProvider;
    private final SubjectAuditRecorder subjectAuditRecorder;
    private final Clock clock;

    public SubjectStatusService(
            ISubjectRepository subjectRepository,
            IAuthorization authorization,
            ObjectProvider<ISubjectClasses> subjectClassesProvider,
            SubjectAuditRecorder subjectAuditRecorder,
            Clock clock) {
        this.subjectRepository = subjectRepository;
        this.subjectAdminAccess =
                new SubjectAdminAccess(authorization, subjectAuditRecorder, clock);
        this.subjectClassesProvider = subjectClassesProvider;
        this.subjectAuditRecorder = subjectAuditRecorder;
        this.clock = clock;
    }

    public record ChangeStatus(SubjectStatus status, Long version) {}

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 1)
    public Subject changeStatus(Actor actor, UUID subjectId, ChangeStatus command) {
        subjectAdminAccess.requireAccess(
                actor, Actions.SUBJECT_STATUS_CHANGE, ResourceRef.subject(subjectId));
        if (subjectId == null || command == null || command.status() == null) {
            throw ApiException.createBadRequest("SUBJECT_STATUS_INVALID", "Cần môn và trạng thái hợp lệ");
        }
        if (command.version() == null || command.version() < 1) {
            throw ApiException.createBadRequest("SUBJECT_VERSION_REQUIRED", "Cần version môn từ 1");
        }
        SubjectLifecycleBudget budget = new SubjectLifecycleBudget(Duration.ofSeconds(1));
        Subject before;
        try {
            before =
                    subjectRepository
                            .findByIdForUpdate(subjectId, budget.calculateRemaining())
                            .orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"));
        } catch (DataAccessException failure) {
            throw ApiException.createUnavailable(
                    "SUBJECT_LIFECYCLE_UNAVAILABLE", "Chưa khóa được môn, vui lòng thử lại");
        }
        before.requireVersion(command.version());
        if (before.status() == command.status()) {
            return before;
        }
        budget.calculateRemaining();
        if (command.status() == SubjectStatus.ARCHIVED) {
            ISubjectClasses provider = subjectClassesProvider.getIfAvailable();
            if (provider == null) {
                throw createClassesUnavailableError();
            }
            // BR-U04-04: lưu trữ môn thì lưu trữ luôn mọi lớp của môn; U05 lỗi thì cả thao tác
            // rollback.
            try {
                if (provider.archiveClassesOfSubject(subjectId, actor) == null) {
                    throw createClassesUnavailableError();
                }
            } catch (RuntimeException failure) {
                throw createClassesUnavailableError();
            }
            budget.calculateRemaining();
        }
        Subject after =
                command.status() == SubjectStatus.ARCHIVED
                        ? before.archive(clock.instant())
                        : before.reopen(clock.instant());
        if (!subjectRepository.updateSubject(after, command.version())) {
            throw ApiException.createConflict(
                    "SUBJECT_VERSION_CONFLICT",
                    "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
        }
        subjectAuditRecorder.recordStatusChanged(actor, before, after);
        return after;
    }

    private ApiException createClassesUnavailableError() {
        return ApiException.createUnavailable(
                "SUBJECT_CLASSES_UNAVAILABLE", "Chưa kiểm tra được lớp, vui lòng thử lại");
    }
}
