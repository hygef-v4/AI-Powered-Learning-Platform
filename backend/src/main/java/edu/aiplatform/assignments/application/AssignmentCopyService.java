package edu.aiplatform.assignments.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentOrigin;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
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

    static final String COPY_SUFFIX = " (bản sao)";

    private final AssignmentRepository assignments;
    private final RubricService rubrics;
    private final AssignmentAccess access;
    private final AssignmentSupport support;
    private final AssignmentAudit audit;
    private final IClassScope classScope;
    private final IReferenceMaterial materials;
    private final Clock clock;

    public AssignmentCopyService(AssignmentRepository assignments, RubricService rubrics, AssignmentAccess access,
                                 AssignmentSupport support, AssignmentAudit audit, IClassScope classScope,
                                 IReferenceMaterial materials, Clock clock) {
        this.assignments = assignments;
        this.rubrics = rubrics;
        this.access = access;
        this.support = support;
        this.audit = audit;
        this.classScope = classScope;
        this.materials = materials;
        this.clock = clock;
    }

    /** BR-U07-40: bản nháp mới cùng lớp hoặc cùng môn. */
    @Transactional
    public AssignmentViews.Detail cloneAssignment(Actor actor, UUID id) {
        Assignment source = support.manageable(actor, id, "ASSIGNMENT_CLONED");
        access.requireOwnerWritable(source);
        requireGroupSupport(source, source.classId());
        Assignment copy = insertCopy(actor, source, source.classId(), source.subjectId(), AssignmentOrigin.CLONE, 1,
                withSuffix(source.title()), source.referenceMaterialIds());
        audit.success(actor, "ASSIGNMENT_CLONED", copy.id(), AssignmentAudit.details("sourceAssignmentId", source.id()));
        return support.detail(actor, copy, List.of());
    }

    /** BR-U07-41: từ bài đã đóng hoặc ngưng giao; mỗi bài nguồn tối đa một version đang soạn. */
    @Transactional
    public AssignmentViews.Detail newVersion(Actor actor, UUID id) {
        Assignment source = support.manageable(actor, id, "ASSIGNMENT_VERSION_CREATED");
        access.requireOwnerWritable(source);
        if (source.status() != AssignmentStatus.CLOSED && source.status() != AssignmentStatus.RETIRED) {
            throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Chỉ tạo version mới từ bài đã đóng hoặc ngưng giao");
        }
        if (assignments.hasOpenVersion(source.id())) {
            throw ApiException.conflict("VERSION_ALREADY_OPEN", "Bài này đã có một version đang soạn");
        }
        requireGroupSupport(source, source.classId());
        Assignment copy;
        try {
            copy = insertCopy(actor, source, source.classId(), source.subjectId(), AssignmentOrigin.NEW_VERSION,
                    source.versionNo() + 1, source.title(), source.referenceMaterialIds());
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("VERSION_ALREADY_OPEN", "Bài này đã có một version đang soạn");
        }
        audit.success(actor, "ASSIGNMENT_VERSION_CREATED", copy.id(), AssignmentAudit.details(
                "sourceAssignmentId", source.id(), "versionNo", copy.versionNo()));
        return support.detail(actor, copy, List.of());
    }

    /** F13 bước 1: lớp mình dạy (trừ lớp đích) và bài của lớp đó. */
    @Transactional(readOnly = true)
    public List<AssignmentViews.CopySource> copySources(Actor actor, UUID targetClassId) {
        access.requireClassManager(actor, targetClassId, "ASSIGNMENT_COPY_SOURCES");
        List<AssignmentViews.CopySource> sources = new ArrayList<>();
        for (UUID classId : classScope.listTaughtClasses(actor.accountId())) {
            if (classId.equals(targetClassId)) {
                continue;
            }
            ClassRef ref = access.classRef(classId);
            List<AssignmentViews.Row> rows = assignments.listOfClass(classId).stream()
                    .map(a -> support.row(a, true)).toList();
            if (!rows.isEmpty()) {
                sources.add(new AssignmentViews.CopySource(classId, ref.code(), ref.name(), ref.status(), rows));
            }
        }
        return sources;
    }

    /** F13 (BR-U07-42, 43): dạy cả lớp nguồn và lớp đích; ngoài quyền thì "không tìm thấy". */
    @Transactional
    public AssignmentViews.Detail copyToClass(Actor actor, UUID targetClassId, UUID sourceAssignmentId) {
        ClassRef target = access.requireClassManager(actor, targetClassId, "ASSIGNMENT_COPIED");
        if (target.status() == ClassStatus.ARCHIVED) {
            throw ApiException.conflict("ASSIGNMENT_READ_ONLY", "Lớp đã lưu trữ, không tạo bài được");
        }
        if (sourceAssignmentId == null) {
            throw ApiException.badRequest("SOURCE_REQUIRED", "Chọn bài để copy");
        }
        Assignment source = assignments.find(sourceAssignmentId).orElseThrow(() -> access.deny(actor,
                "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, sourceAssignmentId, "NOT_FOUND"));
        if (source.scope() != AssignmentScope.CLASS) {
            throw access.deny(actor, "ASSIGNMENT_COPIED", AssignmentAudit.OBJECT_ASSIGNMENT, source.id(),
                    "NOT_CLASS_ASSIGNMENT");
        }
        access.requireClassManager(actor, source.classId(), "ASSIGNMENT_COPIED");
        if (source.classId().equals(targetClassId)) {
            throw ApiException.badRequest("COPY_SAME_CLASS", "Bài đã thuộc lớp này, hãy dùng Nhân bản");
        }
        requireGroupSupport(source, targetClassId);
        Set<UUID> allowed = selectable(actor, ResourceRef.courseClass(targetClassId));
        List<UUID> kept = source.referenceMaterialIds().stream().filter(allowed::contains).toList();
        Assignment copy = insertCopy(actor, source, targetClassId, target.subjectId(), AssignmentOrigin.CLASS_COPY, 1,
                source.title(), kept);
        audit.success(actor, "ASSIGNMENT_COPIED", copy.id(), AssignmentAudit.details("sourceAssignmentId", source.id(),
                "sourceClassId", source.classId(), "targetClassId", targetClassId));
        return support.detail(actor, copy, List.of());
    }

    private Assignment insertCopy(Actor actor, Assignment source, UUID classId, UUID subjectId,
                                  AssignmentOrigin origin, int versionNo, String title, List<UUID> kept) {
        Instant now = clock.instant();
        Assignment copy = source.copyAsDraft(UUID.randomUUID(), classId, subjectId, origin, versionNo, title, kept,
                actor.accountId(), now);
        assignments.insert(copy);
        rubrics.copy(source.id(), copy.id(), now);
        return copy;
    }

    private void requireGroupSupport(Assignment source, UUID classId) {
        if (source.submissionMode() == SubmissionMode.GROUP && !support.groupSupported(classId)) {
            throw ApiException.badRequest("GROUP_NOT_SUPPORTED", "Chưa dùng được bài nhóm cho lớp này");
        }
    }

    private Set<UUID> selectable(Actor actor, ResourceRef scope) {
        try {
            List<SelectableMaterial> list = materials.listSelectable(actor, scope);
            return list == null ? Set.of() : list.stream().map(SelectableMaterial::materialId).collect(Collectors.toSet());
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    static String withSuffix(String title) {
        String base = title.length() + COPY_SUFFIX.length() > Assignment.TITLE_MAX
                ? title.substring(0, Assignment.TITLE_MAX - COPY_SUFFIX.length()) : title;
        return base + COPY_SUFFIX;
    }
}
