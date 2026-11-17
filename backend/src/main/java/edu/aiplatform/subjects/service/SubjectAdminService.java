package edu.aiplatform.subjects.service;

import edu.aiplatform.subjects.dto.SubjectAdminViews;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** UC16/18: authorization trước mọi truy vấn; DTO Admin riêng với contract đọc nội bộ. */
@Service
public class SubjectAdminService {
    private final ISubjectRepository subjectRepository;
    private final SubjectAdminAccess subjectAdminAccess;
    private final IAccountLookup accountLookup;
    private final SubjectClassesReader subjectClassesReader;

    public SubjectAdminService(
            ISubjectRepository subjectRepository,
            IAuthorization authorization,
            IAccountLookup accountLookup,
            SubjectClassesReader subjectClassesReader,
            SubjectAuditRecorder subjectAuditRecorder,
            Clock clock) {
        this.subjectRepository = subjectRepository;
        this.subjectAdminAccess =
                new SubjectAdminAccess(authorization, subjectAuditRecorder, clock);
        this.accountLookup = accountLookup;
        this.subjectClassesReader = subjectClassesReader;
    }

    public SubjectAdminViews.Page listSubjects(
            Actor actor, String query, SubjectStatus status, Integer page, Integer size) {
        subjectAdminAccess.requireAccess(actor, Actions.SUBJECT_LIST, ResourceRef.none());
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw ApiException.createBadRequest(
                    "SUBJECT_PAGE_INVALID", "Trang phải từ 0, số dòng mỗi trang từ 1 đến 100");
        }
        String search = query == null ? null : query.trim();
        List<Subject> subjects =
                subjectRepository.findSubjectPage(search, status, pageNumber, pageSize);
        long total = subjectRepository.countSubjects(search, status);
        Map<SubjectStatus, Long> statusCounts = subjectRepository.countSubjectsByStatus(search);
        Map<UUID, SubjectAdminViews.Manager> managers = loadManagers(subjects);
        Map<UUID, Integer> counts =
                subjectClassesReader.countClasses(subjects.stream().map(Subject::id).toList());
        List<SubjectAdminViews.ListItem> items =
                subjects.stream()
                        .map(
                                subject ->
                                        new SubjectAdminViews.ListItem(
                                                subject.id(),
                                                subject.code().value(),
                                                subject.name(),
                                                subject.status(),
                                                subject.managerId(),
                                                managers.get(subject.managerId()),
                                                counts.get(subject.id())))
                        .toList();
        return new SubjectAdminViews.Page(items, pageNumber, pageSize, total, statusCounts);
    }

    public SubjectAdminViews.Detail loadSubject(Actor actor, UUID subjectId) {
        subjectAdminAccess.requireAccess(
                actor, Actions.SUBJECT_VIEW, ResourceRef.subject(subjectId));
        Subject subject = requireSubject(subjectId);
        var manager = loadManagers(List.of(subject)).get(subject.managerId());
        Integer classCount =
                subjectClassesReader.countClasses(List.of(subject.id())).get(subject.id());
        return new SubjectAdminViews.Detail(
                subject.id(),
                subject.code().value(),
                subject.name(),
                subject.status(),
                subject.managerId(),
                manager,
                classCount,
                subject.description(),
                subject.createdAt(),
                subject.updatedAt(),
                subject.version());
    }

    public SubjectAdminViews.Classes listSubjectClasses(Actor actor, UUID subjectId) {
        subjectAdminAccess.requireAccess(
                actor, Actions.SUBJECT_VIEW, ResourceRef.subject(subjectId));
        Subject subject = requireSubject(subjectId);
        return subjectClassesReader.listClasses(subject.id());
    }

    private Subject requireSubject(UUID id) {
        if (id == null) {
            throw ApiException.createBadRequest("SUBJECT_ID_REQUIRED", "Cần mã định danh môn học");
        }
        return subjectRepository
                .findById(id)
                .orElseThrow(() -> ApiException.createNotFound("SUBJECT_NOT_FOUND"));
    }

    private Map<UUID, SubjectAdminViews.Manager> loadManagers(List<Subject> subjects) {
        Map<UUID, SubjectAdminViews.Manager> result = new HashMap<>();
        subjects.stream()
                .map(Subject::managerId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(
                        id -> {
                            try {
                                accountLookup.getContact(id)
                                        .filter(contact -> id.equals(contact.accountId()))
                                        .ifPresent(
                                                contact ->
                                                        result.put(
                                                                id,
                                                                new SubjectAdminViews.Manager(
                                                                        id,
                                                                        contact.displayName(),
                                                                        contact.email())));
                            } catch (RuntimeException unavailable) {
                                // Chỉ contact hiển thị: giữ managerId, không sửa assignment hoặc
                                // suy ra quyền.
                            }
                        });
        return result;
    }
}
