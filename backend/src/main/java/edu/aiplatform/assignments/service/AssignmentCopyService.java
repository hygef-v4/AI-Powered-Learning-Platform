package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentOrigin;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nhân bản, version mới, copy bài giữa lớp (F12, F13; BR-U07-25, 40…43). */
@Service
public class AssignmentCopyService {

    private final AssignmentRepository assignments;
    private final RubricService rubricService;
    private final AssignmentAccess access;
    private final AssignmentSupport support;
    private final AssignmentAudit audit;
    private final IClassScope classScope;
    private final IReferenceMaterial materials;
    private final Clock clock;

    public AssignmentCopyService(AssignmentRepository assignments, RubricService rubricService, AssignmentAccess access,
                                 AssignmentSupport support, AssignmentAudit audit, IClassScope classScope,
                                 IReferenceMaterial materials, Clock clock) {
        this.assignments = assignments;
        this.rubricService = rubricService;
        this.access = access;
        this.support = support;
        this.audit = audit;
        this.classScope = classScope;
        this.materials = materials;
        this.clock = clock;
    }

    /** BR-U07-40: bản nháp mới cùng lớp hoặc cùng môn; giữ nguyên tên như bài thường (yêu cầu 2026-10-22, bỏ "(bản sao)"). */
    @Transactional
    public AssignmentViews.Detail cloneAssignment(Actor actor, UUID id) {
        Assignment source = support.loadManageable(actor, id, "ASSIGNMENT_CLONED");
        access.requireOwnerWritable(source);
        requireGroupSupport(source, source.classId());
        Assignment copy = insertCopy(actor, source, source.classId(), source.subjectId(), AssignmentOrigin.CLONE, 1,
                source.title(), source.referenceMaterialIds());
        audit.recordSuccess(actor, "ASSIGNMENT_CLONED", copy.id(), AssignmentAudit.collectDetails("sourceAssignmentId", source.id()));
        return support.buildDetail(actor, copy, List.of());
    }

    /** BR-U07-41: từ bài đã đóng hoặc ngưng giao; mỗi bài nguồn tối đa một version đang soạn. */
    @Transactional
    public AssignmentViews.Detail createNewVersion(Actor actor, UUID id) {
        Assignment source = support.loadManageable(actor, id, "ASSIGNMENT_VERSION_CREATED");
        access.requireOwnerWritable(source);
        if (source.status() != AssignmentStatus.CLOSED && source.status() != AssignmentStatus.RETIRED) {
            throw ApiException.createConflict("ASSIGNMENT_STATUS_INVALID", "Chỉ tạo version mới từ bài đã đóng hoặc ngưng giao");
        }
        if (assignments.hasOpenVersion(source.id())) {
            throw ApiException.createConflict("VERSION_ALREADY_OPEN", "Bài này đã có một version đang soạn");
        }
        requireGroupSupport(source, source.classId());
        Assignment copy;
        try {
            copy = insertCopy(actor, source, source.classId(), source.subjectId(), AssignmentOrigin.NEW_VERSION,
                    source.versionNo() + 1, source.title(), source.referenceMaterialIds());
        } catch (DuplicateKeyException e) {
            throw ApiException.createConflict("VERSION_ALREADY_OPEN", "Bài này đã có một version đang soạn");
        }
        audit.recordSuccess(actor, "ASSIGNMENT_VERSION_CREATED", copy.id(), AssignmentAudit.collectDetails(
                "sourceAssignmentId", source.id(), "versionNo", copy.versionNo()));
        return support.buildDetail(actor, copy, List.of());
    }

    /**
     * F13 bước 1, copy vào lớp: đề mẫu của môn (nhóm đầu), rồi các lớp khác mình dạy (trừ lớp đích) và bài của lớp đó.
     */
    @Transactional(readOnly = true)
    public List<AssignmentViews.CopySource> copySources(Actor actor, UUID targetClassId) {
        ClassRef target = access.requireClassManager(actor, targetClassId, "ASSIGNMENT_COPY_SOURCES");
        List<AssignmentViews.CopySource> sources = new ArrayList<>();
        findSubjectSource(target.subjectId(), AssignmentCopyService::isSubjectTemplate).ifPresent(sources::add);
        sources.addAll(listTaughtClassSources(actor, targetClassId));
        return sources;
    }

    /**
     * Copy vào đề mẫu của môn (Chủ nhiệm môn): bài khác của cùng môn (mọi trạng thái), rồi các lớp mình dạy; có cả bài nhóm
     * (yêu cầu 2026-11-09).
     */
    @Transactional(readOnly = true)
    public List<AssignmentViews.CopySource> listSubjectCopySources(Actor actor, UUID subjectId) {
        access.requireSubjectManager(actor, subjectId, "ASSIGNMENT_COPY_SOURCES");
        List<AssignmentViews.CopySource> sources = new ArrayList<>();
        findSubjectSource(subjectId, a -> true).ifPresent(sources::add);
        sources.addAll(listTaughtClassSources(actor, null));
        return sources;
    }

    /** Copy một bài vào bài của môn (nháp mới, phạm vi môn); nguồn là bài cùng môn hoặc bài của lớp mình dạy. */
    @Transactional
    public AssignmentViews.Detail copyToSubject(Actor actor, UUID subjectId, UUID sourceAssignmentId,
                                                String title) {
        return copyToSubject(actor, subjectId, sourceAssignmentId, title, null, null);
    }

    /** {@code submissionMode}, {@code gradingMode}: loại của bài copy; null thì giữ loại của bài nguồn. */
    @Transactional
    public AssignmentViews.Detail copyToSubject(Actor actor, UUID subjectId, UUID sourceAssignmentId, String title,
                                                SubmissionMode submissionMode, GradingMode gradingMode) {
        access.requireSubjectManager(actor, subjectId, "ASSIGNMENT_COPIED");
        if (access.loadSubjectStatus(subjectId) == SubjectStatus.ARCHIVED) {
            throw ApiException.createConflict("ASSIGNMENT_READ_ONLY", "Môn đã lưu trữ, không tạo bài được");
        }
        if (sourceAssignmentId == null) {
            throw ApiException.createBadRequest("SOURCE_REQUIRED", "Chọn bài để copy");
        }
        Assignment source = assignments.findAssignment(sourceAssignmentId).orElseThrow(() -> access.deny(actor,
                "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, sourceAssignmentId, "NOT_FOUND"));
        if (source.scope() == AssignmentScope.SUBJECT) {
            if (!source.subjectId().equals(subjectId)) {
                throw access.deny(actor, "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, source.id(), "OTHER_SUBJECT");
            }
        } else {
            access.requireClassManager(actor, source.classId(), "ASSIGNMENT_COPIED");
        }
        Set<UUID> allowed = listSelectableIds(actor, ResourceRef.subject(subjectId));
        List<UUID> kept = source.referenceMaterialIds().stream().filter(allowed::contains).toList();
        Assignment copy = insertCopy(actor, source.changeModes(submissionMode, gradingMode), null, subjectId,
                AssignmentOrigin.CLASS_COPY, 1, copyTitle(title, source), kept);
        audit.recordSuccess(actor, "ASSIGNMENT_COPIED", copy.id(), AssignmentAudit.collectDetails("sourceAssignmentId", source.id(),
                "targetSubjectId", subjectId));
        return support.buildDetail(actor, copy, List.of());
    }

    /** Bài của môn lọc theo {@code keep}; không có bài nào thì không có nhóm. */
    private java.util.Optional<AssignmentViews.CopySource> findSubjectSource(UUID subjectId,
            java.util.function.Predicate<Assignment> keep) {
        var subject = access.loadSubjectRef(subjectId);
        List<AssignmentViews.Row> rows = assignments.listOfSubject(subjectId).stream()
                .filter(keep)
                .map(a -> support.buildRow(a, true)).toList();
        return rows.isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(new AssignmentViews.CopySource(subjectId, subject.code(), subject.name(), null, rows, "SUBJECT"));
    }

    /** Các lớp mình dạy (trừ {@code exceptClassId}) có ít nhất một bài, mỗi lớp một nhóm. */
    private List<AssignmentViews.CopySource> listTaughtClassSources(Actor actor, UUID exceptClassId) {
        List<AssignmentViews.CopySource> sources = new ArrayList<>();
        for (UUID classId : classScope.listTaughtClasses(actor.accountId())) {
            if (classId.equals(exceptClassId)) {
                continue;
            }
            ClassRef ref = access.loadClassRef(classId);
            List<AssignmentViews.Row> rows = assignments.listOfClass(classId).stream()
                    .map(a -> support.buildRow(a, true)).toList();
            if (!rows.isEmpty()) {
                sources.add(new AssignmentViews.CopySource(classId, ref.code(), ref.name(), ref.status(), rows,
                        "CLASS", ref.term()));
            }
        }
        return sources;
    }

    /** F13 (BR-U07-42, 43): dạy cả lớp nguồn và lớp đích; ngoài quyền thì "không tìm thấy". */
    @Transactional
    public AssignmentViews.Detail copyToClass(Actor actor, UUID targetClassId, UUID sourceAssignmentId,
                                              String title) {
        return copyToClass(actor, targetClassId, sourceAssignmentId, title, null, null);
    }

    /** {@code submissionMode}, {@code gradingMode}: loại của bài copy; null thì giữ loại của bài nguồn. */
    @Transactional
    public AssignmentViews.Detail copyToClass(Actor actor, UUID targetClassId, UUID sourceAssignmentId, String title,
                                              SubmissionMode submissionMode, GradingMode gradingMode) {
        ClassRef target = access.requireClassManager(actor, targetClassId, "ASSIGNMENT_COPIED");
        if (target.status() == ClassStatus.ARCHIVED) {
            throw ApiException.createConflict("ASSIGNMENT_READ_ONLY", "Lớp đã lưu trữ, không tạo bài được");
        }
        if (sourceAssignmentId == null) {
            throw ApiException.createBadRequest("SOURCE_REQUIRED", "Chọn bài để copy");
        }
        Assignment source = assignments.findAssignment(sourceAssignmentId).orElseThrow(() -> access.deny(actor,
                "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, sourceAssignmentId, "NOT_FOUND"));
        if (source.scope() == AssignmentScope.SUBJECT) {
            // Đề mẫu của cùng môn (yêu cầu 2026-11-09: bài của môn không còn phát hành cho các lớp).
            if (!source.subjectId().equals(target.subjectId()) || !isSubjectTemplate(source)) {
                throw access.deny(actor, "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, source.id(),
                        "OUT_OF_ASSIGNMENT_SCOPE");
            }
        } else {
            access.requireClassManager(actor, source.classId(), "ASSIGNMENT_COPIED");
            if (source.classId().equals(targetClassId)) {
                throw ApiException.createBadRequest("COPY_SAME_CLASS", "Bài đã thuộc lớp này, hãy dùng Nhân bản");
            }
        }
        source = source.changeModes(submissionMode, gradingMode);
        requireGroupSupport(source, targetClassId);
        Set<UUID> allowed = listSelectableIds(actor, ResourceRef.courseClass(targetClassId));
        List<UUID> kept = source.referenceMaterialIds().stream().filter(allowed::contains).toList();
        Assignment copy = insertCopy(actor, source, targetClassId, target.subjectId(), AssignmentOrigin.CLASS_COPY, 1,
                copyTitle(title, source), kept);
        audit.recordSuccess(actor, "ASSIGNMENT_COPIED", copy.id(), source.classId() == null
                ? AssignmentAudit.collectDetails("sourceAssignmentId", source.id(), "targetClassId", targetClassId)
                : AssignmentAudit.collectDetails("sourceAssignmentId", source.id(), "sourceClassId", source.classId(),
                        "targetClassId", targetClassId));
        return support.buildDetail(actor, copy, List.of());
    }

    /** Bài của môn dùng làm đề mẫu cho lớp: mọi trạng thái trừ đã ngưng giao. */
    static boolean isSubjectTemplate(Assignment assignment) {
        return assignment.scope() == AssignmentScope.SUBJECT && assignment.status() != AssignmentStatus.RETIRED;
    }

    private static String copyTitle(String title, Assignment source) {
        return title == null || title.isBlank() ? source.title() : AssignmentAuthoringService.normalizeTitle(title);
    }

    private Assignment insertCopy(Actor actor, Assignment source, UUID classId, UUID subjectId,
                                  AssignmentOrigin origin, int versionNo, String title, List<UUID> kept) {
        Instant now = clock.instant();
        Assignment copy = source.copyAsDraft(UUID.randomUUID(), classId, subjectId, origin, versionNo, title, kept,
                actor.accountId(), now);
        assignments.insert(copy);
        rubricService.copy(source.id(), copy.id(), now);
        return copy;
    }

    private void requireGroupSupport(Assignment source, UUID classId) {
        if (source.submissionMode() == SubmissionMode.GROUP && !support.groupSupported(classId)) {
            throw ApiException.createBadRequest("GROUP_NOT_SUPPORTED", "Chưa dùng được bài nhóm cho lớp này");
        }
    }

    private Set<UUID> listSelectableIds(Actor actor, ResourceRef scope) {
        try {
            List<SelectableMaterial> list = materials.listSelectable(actor, scope);
            return list == null ? Set.of() : list.stream().map(SelectableMaterial::materialId).collect(Collectors.toSet());
        } catch (RuntimeException e) {
            return Set.of();
        }
    }
}
