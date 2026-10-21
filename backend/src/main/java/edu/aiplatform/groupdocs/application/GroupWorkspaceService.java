package edu.aiplatform.groupdocs.application;

import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.groupdocs.application.GroupSectionService.SectionView;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groups.application.GroupAccess;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mở workspace nhóm (F8): tài liệu kèm phần chung, mục, người giữ, nội dung đã đăng, quyền của người gọi, bản nộp
 * gần nhất. Người học mở trước khi job chạy xong thì tài liệu được tạo ngay (F7 bước 3).
 */
@Service
public class GroupWorkspaceService {

    private final GroupDocumentRepository documents;
    private final GroupRepository groups;
    private final GroupDocAccess docAccess;
    private final GroupAccess access;
    private final GroupDocumentService creator;
    private final IAssignmentQuery assignments;
    private final IAccountLookup accounts;

    public GroupWorkspaceService(GroupDocumentRepository documents, GroupRepository groups, GroupDocAccess docAccess,
                                 GroupAccess access, GroupDocumentService creator, IAssignmentQuery assignments,
                                 IAccountLookup accounts) {
        this.documents = documents;
        this.groups = groups;
        this.docAccess = docAccess;
        this.access = access;
        this.creator = creator;
        this.assignments = assignments;
        this.accounts = accounts;
    }

    /** Sinh viên mở bài nhóm từ Assignment Detail: tìm nhóm của mình trong lớp của bài. */
    @Transactional
    public WorkspaceView myGroupDocument(Actor actor, UUID assignmentId) {
        AssignmentView assignment = assignments.get(assignmentId).filter(a -> a.classId() != null)
                .orElseThrow(GroupAccess::notFound);
        access.requireStudent(actor, assignment.classId());
        ClassGroup group = groups.findGroupOf(assignment.classId(), actor.accountId())
                .orElseThrow(() -> ApiException.notFound("GROUP_NONE"));
        GroupDocument document = creator.ensureFor(group, assignmentId).orElseThrow(GroupAccess::notFound);
        return view(docAccess.context(actor, document), assignment);
    }

    @Transactional(readOnly = true)
    public WorkspaceView workspace(Actor actor, UUID documentId) {
        GroupDocContext ctx = docAccess.read(actor, documentId);
        AssignmentView assignment = assignments.get(ctx.document().assignmentId()).orElse(null);
        return view(ctx, assignment);
    }

    private WorkspaceView view(GroupDocContext ctx, AssignmentView assignment) {
        GroupDocument d = ctx.document();
        List<SectionView> sections = documents.listSections(d.id()).stream()
                .map(s -> SectionView.of(s, ctx.actorId())).toList();
        List<Member> members = ctx.group().memberIds().stream().map(id -> member(id, ctx.group())).toList();
        SubmissionWindow window = assignment == null ? SubmissionWindow.CLOSED
                : docAccess.window(d.assignmentId(), false);
        Submission submission = d.submittedAt() == null ? null : new Submission(d.submittedBy(), d.submitMode().name(),
                d.submittedAt(), d.late(), d.receiptHash());
        return new WorkspaceView(d.id(), d.assignmentId(), assignment == null ? null : assignment.title(),
                assignment == null ? null : assignment.closesAt(), assignment == null ? null : assignment.lateUntil(),
                d.classId(), ctx.group().id(), ctx.group().name(), ctx.group().leaderId(), members, d.status().name(),
                window.name(), d.sharedBlocks(), sections, new Role(ctx.member(), ctx.leader(), ctx.teacher()),
                submission, d.activityMonitoring(), d.version());
    }

    private Member member(UUID id, ClassGroup group) {
        Optional<AccountContact> contact;
        try {
            contact = accounts.getContact(id);
        } catch (RuntimeException e) {
            contact = Optional.empty();
        }
        return new Member(id, contact.map(AccountContact::displayName).orElse(null), id.equals(group.leaderId()));
    }

    public record WorkspaceView(UUID documentId, UUID assignmentId, String assignmentTitle, Instant closesAt,
                                Instant lateUntil, UUID classId, UUID groupId, String groupName, UUID leaderId,
                                List<Member> members, String status, String window, DocumentModel sharedBlocks,
                                List<SectionView> sections, Role role, Submission lastSubmission,
                                boolean activityMonitoring, long version) {
    }

    public record Member(UUID accountId, String name, boolean leader) {
    }

    public record Role(boolean member, boolean leader, boolean teacher) {
    }

    public record Submission(UUID submittedBy, String mode, Instant submittedAt, boolean late, String receiptHash) {
    }
}
