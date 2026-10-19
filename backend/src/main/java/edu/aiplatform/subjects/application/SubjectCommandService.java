package edu.aiplatform.subjects.application;

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
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC17/19. Tách command để transaction không chờ contact/classes hiển thị của B07. */
@Service
public class SubjectCommandService {
    private final ISubjectRepository repository;
    private final SubjectAdminAccess access;
    private final IAccountLookup accounts;
    private final SubjectAuditRecorder audit;
    private final Clock clock;

    public SubjectCommandService(ISubjectRepository repository, IAuthorization authorization, IAccountLookup accounts,
                                 SubjectAuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.access = new SubjectAdminAccess(authorization, audit, clock);
        this.accounts = accounts;
        this.audit = audit;
        this.clock = clock;
    }

    public record Create(String code, String name, String description, UUID managerId) { }
    public record UpdateInformation(String name, String description, Long version) { }
    public record ChangeManager(UUID managerId, Long version) { }

    @Transactional
    public Subject create(Actor actor, Create command) {
        access.require(actor, Actions.SUBJECT_CREATE, ResourceRef.none());
        requireCommand(command);
        Subject subject = Subject.create(UUID.randomUUID(), command.code(), command.name(), command.description(),
                command.managerId(), clock.instant());
        if (repository.existsByCode(subject.code())) {
            throw ApiException.conflict("SUBJECT_CODE_DUPLICATE", "Mã môn đã tồn tại, hãy dùng mã khác");
        }
        if (subject.managerId() != null) {
            requireEligibleManager(subject.managerId());
        }
        try {
            repository.insert(subject);
        } catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && "uk_subjects_code".equals(constraint.getConstraintName())) {
                    throw ApiException.conflict("SUBJECT_CODE_DUPLICATE", "Mã môn đã tồn tại, hãy dùng mã khác");
                }
            }
            throw failure;
        }
        audit.created(actor, subject);
        return subject;
    }

    @Transactional
    public Subject updateInformation(Actor actor, UUID subjectId, UpdateInformation command) {
        access.require(actor, Actions.SUBJECT_UPDATE, ResourceRef.subject(subjectId));
        requireCommand(command);
        long version = requireVersion(command.version());
        Subject before = subject(subjectId);
        before.requireVersion(version);
        Subject after = before.updateInformation(command.name(), command.description(), clock.instant());
        if (after == before) {
            return before;
        }
        save(after, version);
        audit.informationUpdated(actor, before, after);
        return after;
    }

    @Transactional
    public Subject changeManager(Actor actor, UUID subjectId, ChangeManager command) {
        access.require(actor, Actions.SUBJECT_MANAGER_CHANGE, ResourceRef.subject(subjectId));
        requireCommand(command);
        long version = requireVersion(command.version());
        Subject preliminary = subject(subjectId);
        preliminary.requireVersion(version);
        if (Objects.equals(preliminary.managerId(), command.managerId())) {
            return preliminary;
        }
        Subject before = repository.findByIdForUpdate(subjectId)
                .orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"));
        before.requireVersion(version);
        if (command.managerId() != null) {
            if (before.status() == SubjectStatus.ARCHIVED) {
                throw ApiException.conflict("SUBJECT_ARCHIVED", "Môn đã lưu trữ không có Chủ nhiệm môn. Hãy mở lại môn trước");
            }
            requireEligibleManager(command.managerId());
        }
        Subject after = before.changeManager(command.managerId(), clock.instant());
        save(after, version);
        audit.managerChanged(actor, before, after);
        return after;
    }

    private void requireEligibleManager(UUID id) {
        AccountContact contact;
        try {
            contact = accounts.getContact(id).orElse(null);
        } catch (RuntimeException unavailable) {
            throw ApiException.unavailable("SUBJECT_ACCOUNT_LOOKUP_UNAVAILABLE", "Chưa kiểm tra được tài khoản, vui lòng thử lại");
        }
        if (contact == null || !id.equals(contact.accountId())
                || contact.role() != Role.SUBJECT_MANAGER || contact.status() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("SUBJECT_MANAGER_INVALID", "Chủ nhiệm môn phải là tài khoản Chủ nhiệm môn đang hoạt động");
        }
    }

    private Subject subject(UUID id) {
        if (id == null) {
            throw ApiException.badRequest("SUBJECT_ID_REQUIRED", "Cần mã định danh môn học");
        }
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"));
    }

    private void save(Subject after, long version) {
        if (!repository.update(after, version)) {
            throw ApiException.conflict("SUBJECT_VERSION_CONFLICT", "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
        }
    }

    private long requireVersion(Long version) {
        if (version == null || version < 1) {
            throw ApiException.badRequest("SUBJECT_VERSION_REQUIRED", "Cần version môn từ 1");
        }
        return version;
    }

    private void requireCommand(Object command) {
        if (command == null) {
            throw ApiException.badRequest("SUBJECT_COMMAND_REQUIRED", "Cần dữ liệu thao tác môn học");
        }
    }
}
