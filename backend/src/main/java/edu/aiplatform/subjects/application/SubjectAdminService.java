package edu.aiplatform.subjects.application;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** UC16/18: authorization trước mọi truy vấn; DTO Admin riêng với contract đọc nội bộ. */
@Service
public class SubjectAdminService {
    private final ISubjectRepository repository;
    private final SubjectAdminAccess access;
    private final IAccountLookup accounts;
    private final SubjectClassesReader classes;

    public SubjectAdminService(ISubjectRepository repository, IAuthorization authorization,
                               IAccountLookup accounts, SubjectClassesReader classes, SubjectAuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.access = new SubjectAdminAccess(authorization, audit, clock);
        this.accounts = accounts;
        this.classes = classes;
    }

    public SubjectAdminViews.Page list(Actor actor, String query, SubjectStatus status, Integer page, Integer size) {
        access.require(actor, "SUBJECT_LIST", ResourceRef.none());
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw ApiException.badRequest("SUBJECT_PAGE_INVALID", "Trang phải từ 0, số dòng mỗi trang từ 1 đến 100");
        }
        String search = query == null ? null : query.trim();
        List<Subject> subjects = repository.findPage(search, status, pageNumber, pageSize);
        long total = repository.count(search, status);
        Map<SubjectStatus, Long> statusCounts = repository.countByStatus(search);
        Map<UUID, SubjectAdminViews.Manager> managers = managers(subjects);
        Map<UUID, Integer> counts = classes.counts(subjects.stream().map(Subject::id).toList());
        List<SubjectAdminViews.ListItem> items = subjects.stream().map(subject ->
                new SubjectAdminViews.ListItem(subject.id(), subject.code().value(), subject.name(), subject.status(),
                        subject.managerId(), managers.get(subject.managerId()), counts.get(subject.id()))).toList();
        return new SubjectAdminViews.Page(items, pageNumber, pageSize, total, statusCounts);
    }

    public SubjectAdminViews.Detail detail(Actor actor, UUID subjectId) {
        access.require(actor, "SUBJECT_VIEW", ResourceRef.subject(subjectId));
        Subject subject = subject(subjectId);
        var manager = managers(List.of(subject)).get(subject.managerId());
        Integer classCount = classes.counts(List.of(subject.id())).get(subject.id());
        return new SubjectAdminViews.Detail(subject.id(), subject.code().value(), subject.name(), subject.status(),
                subject.managerId(), manager, classCount, subject.description(),
                subject.createdAt(), subject.updatedAt(), subject.version());
    }

    public SubjectAdminViews.Classes classes(Actor actor, UUID subjectId) {
        access.require(actor, "SUBJECT_VIEW", ResourceRef.subject(subjectId));
        Subject subject = subject(subjectId);
        return classes.classes(subject.id());
    }

    private Subject subject(UUID id) {
        if (id == null) {
            throw ApiException.badRequest("SUBJECT_ID_REQUIRED", "Cần mã định danh môn học");
        }
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND"));
    }

    private Map<UUID, SubjectAdminViews.Manager> managers(List<Subject> subjects) {
        Map<UUID, SubjectAdminViews.Manager> result = new HashMap<>();
        subjects.stream().map(Subject::managerId).filter(java.util.Objects::nonNull).distinct().forEach(id -> {
            try {
                accounts.getContact(id).filter(contact -> id.equals(contact.accountId())).ifPresent(contact ->
                        result.put(id, new SubjectAdminViews.Manager(id, contact.displayName(), contact.email())));
            } catch (RuntimeException unavailable) {
                // Chỉ contact hiển thị: giữ managerId, không sửa assignment hoặc suy ra quyền.
            }
        });
        return result;
    }

}
