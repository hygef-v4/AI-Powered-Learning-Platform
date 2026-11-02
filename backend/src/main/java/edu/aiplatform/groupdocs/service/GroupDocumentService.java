package edu.aiplatform.groupdocs.service;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo tài liệu nhóm cho bài nhóm đang mở (F7, BR-U12-30): mỗi nhóm có thành viên hiệu lực một tài liệu, mỗi phần
 * của khung một mục, phần chung chỉ đọc. Tạo lại không trùng ({@code ON CONFLICT DO NOTHING}, P3).
 */
@Service
public class GroupDocumentService {

    private final GroupDocumentRepository documents;
    private final GroupRepository groups;
    private final IAssignmentQuery assignments;
    private final IClassAccess classes;
    private final Clock clock;

    public GroupDocumentService(GroupDocumentRepository documents, GroupRepository groups, IAssignmentQuery assignments,
                                IClassAccess classes, Clock clock) {
        this.documents = documents;
        this.groups = groups;
        this.assignments = assignments;
        this.classes = classes;
        this.clock = clock;
    }

    /** Job {@code GROUP_DOC_CREATE}: tài liệu cho mọi nhóm của lớp; trả số tài liệu mới tạo. */
    @Transactional
    public int ensureDocuments(UUID assignmentId) {
        Optional<AssignmentView> view = openGroupAssignment(assignmentId);
        if (view.isEmpty()) {
            return 0;
        }
        AssignmentContent content = assignments.getContent(assignmentId).orElse(null);
        if (content == null) {
            return 0;
        }
        Set<UUID> active = new HashSet<>(classes.listActiveStudents(view.get().classId()));
        int created = 0;
        for (ClassGroup group : groups.listGroups(view.get().classId())) {
            if (group.memberIds().stream().anyMatch(active::contains) && create(group, view.get(), content)) {
                created++;
            }
        }
        return created;
    }

    /** Nhóm mới tạo khi lớp có bài nhóm đang mở cũng có tài liệu (F7 bước 4, F14). */
    @Transactional
    public void ensureForGroups(UUID classId, Collection<UUID> groupIds) {
        if (groupIds.isEmpty()) {
            return;
        }
        for (AssignmentView view : assignments.listForClass(classId)) {
            if (!isOpenGroup(view)) {
                continue;
            }
            Optional<AssignmentContent> content = assignments.getContent(view.assignmentId());
            if (content.isEmpty()) {
                continue;
            }
            for (UUID groupId : groupIds) {
                groups.findGroup(groupId).ifPresent(group -> create(group, view, content.get()));
            }
        }
    }

    /** Người học mở trước khi job chạy xong thì tạo ngay khi truy cập (F7 bước 3). */
    @Transactional
    public Optional<GroupDocument> ensureFor(ClassGroup group, UUID assignmentId) {
        Optional<GroupDocument> existing = documents.findByGroupAndAssignment(group.id(), assignmentId);
        if (existing.isPresent()) {
            return existing;
        }
        Optional<AssignmentView> view = openGroupAssignment(assignmentId)
                .filter(v -> group.classId().equals(v.classId()));
        Optional<AssignmentContent> content = view.flatMap(v -> assignments.getContent(assignmentId));
        if (view.isEmpty() || content.isEmpty()) {
            return Optional.empty();
        }
        create(group, view.get(), content.get());
        return documents.findByGroupAndAssignment(group.id(), assignmentId);
    }

    private boolean create(ClassGroup group, AssignmentView view, AssignmentContent content) {
        Instant now = clock.instant();
        DocumentModel shared = content.skeleton() == null ? new DocumentModel(List.of()) : content.skeleton();
        GroupDocument document = GroupDocument.create(UUID.randomUUID(), group.id(), view.assignmentId(), group.classId(),
                shared, view.activityMonitoringEnabled(), now);
        if (!documents.insertIfAbsent(document)) {
            return false;
        }
        List<DocumentPart> parts = content.parts() == null ? List.of() : content.parts();
        for (int i = 0; i < parts.size(); i++) {
            DocumentPart part = parts.get(i);
            documents.insertSection(new GroupSection(UUID.randomUUID(), document.id(), part.partId(), part.headingBlockId(),
                    part.ancestorHeadingIds(), i + 1, part.title(), GroupSection.Status.OPEN, null, null, null, null, null,
                    null, null, 0));
        }
        return true;
    }

    private Optional<AssignmentView> openGroupAssignment(UUID assignmentId) {
        return assignments.get(assignmentId).filter(GroupDocumentService::isOpenGroup);
    }

    static boolean isOpenGroup(AssignmentView view) {
        return view.submissionMode() == SubmissionMode.GROUP && view.status() == AssignmentStatus.OPEN
                && view.classId() != null;
    }
}
