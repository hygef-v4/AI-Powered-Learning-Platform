package edu.aiplatform.subjects.service;

import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC17/19. Tách command để transaction không chờ contact/classes hiển thị của B07. */
@Service
public class SubjectCommandService {
    private final ISubjectRepository subjectRepository;
    private final SubjectAdminAccess subjectAdminAccess;
    private final IAccountLookup accountLookup;
    private final SubjectAuditRecorder subjectAuditRecorder;
    private final Clock clock;

    public SubjectCommandService(
            ISubjectRepository subjectRepository,
            IAuthorization authorization,
            IAccountLookup accountLookup,
            SubjectAuditRecorder subjectAuditRecorder,
            Clock clock) {
        this.subjectRepository = subjectRepository;
        this.subjectAdminAccess =
                new SubjectAdminAccess(authorization, subjectAuditRecorder, clock);
        this.accountLookup = accountLookup;
        this.subjectAuditRecorder = subjectAuditRecorder;
        this.clock = clock;
    }

    public record Create(String code, String name, String description, UUID managerId) {}

    public record UpdateInformation(String name, String description, Long version) {}

    public record ChangeManager(UUID managerId, Long version) {}

    @Transactional
    public Subject createSubject(Actor actor, Create command) {
        subjectAdminAccess.requireAccess(actor, Actions.SUBJECT_CREATE, ResourceRef.none());
        requireCommand(command);
        Subject subject =
                Subject.createSubject(
                        UUID.randomUUID(),
                        command.code(),
                        command.name(),
                        command.description(),
                        command.managerId(),
                        clock.instant());
        if (subjectRepository.hasSubjectWithCode(subject.code())) {
            throw ApiException.createConflict(
                    "SUBJECT_CODE_DUPLICATE", "Mã môn đã tồn tại, hãy dùng mã khác");
        }
        if (subject.managerId() != null) {
            requireEligibleManager(subject.managerId(), lockManagerAccount(subject.managerId()));
        }
        try {
            subjectRepository.insertSubject(subject);
        } catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && "uk_subjects_code".equals(constraint.getConstraintName())) {
                    throw ApiException.createConflict(
                            "SUBJECT_CODE_DUPLICATE", "Mã môn đã tồn tại, hãy dùng mã khác");
                }
            }
            throw failure;
        }
        subjectAuditRecorder.recordCreated(actor, subject);
        return subject;
    }

    @Transactional
    public Subject updateInformation(Actor actor, UUID subjectId, UpdateInformation command) {
        subjectAdminAccess.requireAccess(
                actor, Actions.SUBJECT_UPDATE, ResourceRef.subject(subjectId));
        requireCommand(command);
        long version = requireVersion(command.version());
        Subject before = requireSubject(subjectId);
        before.requireVersion(version);
        Subject after =
                before.updateInformation(command.name(), command.description(), clock.instant());
        if (after == before) {
            return before;
        }
        saveSubject(after, version);
        subjectAuditRecorder.recordInformationUpdated(actor, before, after);
        return after;
    }

    @Transactional
    public Subject changeManager(Actor actor, UUID subjectId, ChangeManager command) {
        subjectAdminAccess.requireAccess(
                actor, Actions.SUBJECT_MANAGER_CHANGE, ResourceRef.subject(subjectId));
        requireCommand(command);
        long version = requireVersion(command.version());
        Subject preliminary = requireSubject(subjectId);
        preliminary.requireVersion(version);
        if (Objects.equals(preliminary.managerId(), command.managerId())) {
            return preliminary;
        }
        if (command.managerId() != null && preliminary.status() == SubjectStatus.ARCHIVED) {
            throw createArchivedSubjectError();
        }
        // Khóa tài khoản trước dòng môn, cùng thứ tự với U01 khi vô hiệu hóa tài khoản rồi gỡ phân
        // công; điều kiện của
        // tài khoản kiểm sau môn để giữ thứ tự lỗi (version, lưu trữ, rồi mới tới Chủ nhiệm môn).
        AccountContact manager =
                command.managerId() == null ? null : lockManagerAccount(command.managerId());
        Subject before =
                subjectRepository
                        .findByIdForUpdate(subjectId)
                        .orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"));
        before.requireVersion(version);
        if (command.managerId() != null) {
            if (before.status() == SubjectStatus.ARCHIVED) {
                throw createArchivedSubjectError();
            }
            requireEligibleManager(command.managerId(), manager);
        }
        Subject after = before.changeManager(command.managerId(), clock.instant());
        saveSubject(after, version);
        subjectAuditRecorder.recordManagerChanged(actor, before, after);
        return after;
    }

    private static ApiException createArchivedSubjectError() {
        return ApiException.createConflict(
                "SUBJECT_ARCHIVED", "Môn đã lưu trữ không có Chủ nhiệm môn. Hãy mở lại môn trước");
    }

    /** Đọc và khóa tài khoản tới hết transaction (U01 {@code lockContact}) để role không đổi trước khi gán xong. */
    private AccountContact lockManagerAccount(UUID id) {
        try {
            return accountLookup.lockContact(id).orElse(null);
        } catch (RuntimeException unavailable) {
            throw ApiException.createUnavailable(
                    "SUBJECT_ACCOUNT_LOOKUP_UNAVAILABLE",
                    "Chưa kiểm tra được tài khoản, vui lòng thử lại");
        }
    }

    private static void requireEligibleManager(UUID id, AccountContact contact) {
        if (contact == null
                || !id.equals(contact.accountId())
                || contact.role() != Role.SUBJECT_MANAGER
                || contact.status() != AccountStatus.ACTIVE) {
            throw ApiException.createBadRequest(
                    "SUBJECT_MANAGER_INVALID",
                    "Chủ nhiệm môn phải là tài khoản Chủ nhiệm môn đang hoạt động");
        }
    }

    private Subject requireSubject(UUID id) {
        if (id == null) {
            throw ApiException.createBadRequest("SUBJECT_ID_REQUIRED", "Cần mã định danh môn học");
        }
        return subjectRepository
                .findById(id)
                .orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"));
    }

    private void saveSubject(Subject after, long version) {
        if (!subjectRepository.updateSubject(after, version)) {
            throw ApiException.createConflict(
                    "SUBJECT_VERSION_CONFLICT",
                    "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
        }
    }

    private long requireVersion(Long version) {
        if (version == null || version < 1) {
            throw ApiException.createBadRequest("SUBJECT_VERSION_REQUIRED", "Cần version môn từ 1");
        }
        return version;
    }

    private void requireCommand(Object command) {
        if (command == null) {
            throw ApiException.createBadRequest(
                    "SUBJECT_COMMAND_REQUIRED", "Cần dữ liệu thao tác môn học");
        }
    }
}
