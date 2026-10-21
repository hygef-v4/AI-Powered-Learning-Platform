package edu.aiplatform.groups.application;

import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhóm của tôi (F4, BR-U12-50): nhóm, thành viên, trưởng nhóm, tài liệu của các bài nhóm đã mở kèm tiến độ và nộp,
 * yêu cầu đổi trưởng nhóm gần nhất của chính mình. Không thấy nhóm khác hay yêu cầu của người khác. Giảng viên xem
 * nhóm của một sinh viên trên popup Student Detail (BR-U12-51).
 */
@Service
public class MyGroupService {

    private final GroupRepository groups;
    private final GroupDocumentRepository documents;
    private final GroupAccess access;
    private final IAssignmentQuery assignments;
    private final IAccountLookup accounts;

    public MyGroupService(GroupRepository groups, GroupDocumentRepository documents, GroupAccess access,
                          IAssignmentQuery assignments, IAccountLookup accounts) {
        this.groups = groups;
        this.documents = documents;
        this.access = access;
        this.assignments = assignments;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public MyGroupView myGroup(Actor actor, UUID classId) {
        var ref = access.requireStudent(actor, classId);
        Optional<ClassGroup> found = groups.findGroupOf(classId, actor.accountId());
        if (found.isEmpty()) {
            return new MyGroupView(classId, ref.status().name(), null, null, null, List.of(), List.of(), false, List.of());
        }
        ClassGroup group = found.get();
        List<Member> members = group.memberIds().stream().map(id -> member(id, group)).toList();
        Map<UUID, GroupDocument> docs = documents.listByGroup(group.id()).stream()
                .collect(Collectors.toMap(GroupDocument::assignmentId, Function.identity(), (a, b) -> a));
        List<GroupWork> works = assignments.listForClass(classId).stream()
                .filter(a -> a.submissionMode() == SubmissionMode.GROUP && a.status() != AssignmentStatus.DRAFT
                        && a.status() != AssignmentStatus.REVIEWED && a.status() != AssignmentStatus.SCHEDULED)
                .map(a -> work(a, docs.get(a.assignmentId()))).toList();
        List<RequestSummary> mine = groups.latestOf(group.id(), actor.accountId(), 2).stream()
                .map(RequestSummary::of).toList();
        return new MyGroupView(classId, ref.status().name(), group.id(), group.name(), group.leaderId(), members, works,
                groups.findPending(group.id()).isPresent(), mine);
    }

    /** Nhóm của một sinh viên cho popup Student Detail của giảng viên. */
    @Transactional(readOnly = true)
    public StudentGroup studentGroup(Actor actor, UUID classId, UUID studentId) {
        access.requireTeacher(actor, classId);
        return groups.findGroupOf(classId, studentId)
                .map(g -> new StudentGroup(g.id(), g.name(), studentId.equals(g.leaderId()), g.memberIds().size()))
                .orElse(new StudentGroup(null, null, false, 0));
    }

    private GroupWork work(AssignmentView a, GroupDocument d) {
        if (d == null) {
            return new GroupWork(a.assignmentId(), a.title(), a.closesAt(), null, 0, 0, false, null, false);
        }
        List<GroupSection> sections = documents.listSections(d.id());
        int done = (int) sections.stream().filter(s -> s.status() == GroupSection.Status.DONE).count();
        return new GroupWork(a.assignmentId(), a.title(), a.closesAt(), d.id(), done, sections.size(), d.submitted(),
                d.submittedAt(), d.late());
    }

    private Member member(UUID id, ClassGroup group) {
        Optional<AccountContact> c;
        try {
            c = accounts.getContact(id);
        } catch (RuntimeException e) {
            c = Optional.empty();
        }
        return new Member(id, c.map(AccountContact::displayName).orElse(null), c.map(AccountContact::email).orElse(null),
                id.equals(group.leaderId()));
    }

    public record MyGroupView(UUID classId, String classStatus, UUID groupId, String name, UUID leaderId,
                              List<Member> members, List<GroupWork> works, boolean pendingRequest,
                              List<RequestSummary> myRequests) {
    }

    public record Member(UUID accountId, String name, String email, boolean leader) {
    }

    public record GroupWork(UUID assignmentId, String title, Instant closesAt, UUID documentId, int doneSections,
                            int totalSections, boolean submitted, Instant submittedAt, boolean late) {
    }

    public record RequestSummary(UUID id, LeaderChangeRequest.Status status, UUID proposedLeaderId, String reason,
                                 String decisionReason, Instant createdAt, Instant decidedAt) {

        static RequestSummary of(LeaderChangeRequest r) {
            return new RequestSummary(r.id(), r.status(), r.proposedLeaderId(), r.reason(), r.decisionReason(),
                    r.createdAt(), r.decidedAt());
        }
    }

    public record StudentGroup(UUID groupId, String name, boolean leader, int memberCount) {
    }
}
