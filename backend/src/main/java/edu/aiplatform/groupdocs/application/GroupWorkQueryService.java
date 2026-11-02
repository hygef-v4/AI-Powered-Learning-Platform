package edu.aiplatform.groupdocs.application;

import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.GroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.grading.contract.GroupSectionAuthor;
import edu.aiplatform.grading.contract.GroupSubmissionStatus;
import edu.aiplatform.grading.contract.GroupSubmissionView;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.application.GroupAccess;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bài nhóm cho unit khác (F15; BR-U12-38, 51, 52): trạng thái cho Student Assignments (U08), bản nộp và tiến độ
 * cho chấm (U09), tiến độ cho tab Students, ghi hoạt động qua U08 sau khi kiểm thành viên.
 */
@Service
public class GroupWorkQueryService implements IGroupWorkStatus, IGroupSubmissionSource {

    public static final int MAX_ACTIVITY_EVENTS = 200;

    private final GroupDocumentRepository documents;
    private final GroupRepository groups;
    private final GroupDocAccess docAccess;
    private final GroupAccess access;
    private final IActivityWrite activity;
    private final IAssignmentQuery assignments;

    public GroupWorkQueryService(GroupDocumentRepository documents, GroupRepository groups, GroupDocAccess docAccess,
                                 GroupAccess access, IActivityWrite activity, IAssignmentQuery assignments) {
        this.assignments = assignments;
        this.documents = documents;
        this.groups = groups;
        this.docAccess = docAccess;
        this.access = access;
        this.activity = activity;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, GroupWorkStatus> statusFor(UUID accountId, Collection<UUID> assignmentIds) {
        Map<UUID, GroupWorkStatus> result = new LinkedHashMap<>();
        for (GroupDocument d : documents.listByGroups(groups.listGroupIdsOf(accountId), assignmentIds)) {
            result.put(d.assignmentId(), new GroupWorkStatus(d.assignmentId(), d.id(), d.submitted(), d.submittedAt()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupSubmissionView> getGroupSubmission(UUID groupDocumentId) {
        return documents.findById(groupDocumentId).filter(GroupDocument::submitted).flatMap(d -> groups.findGroup(d.groupId())
                .map(g -> new GroupSubmissionView(d.id(), g.id(), d.assignmentId(), d.classId(), d.submittedDocument(),
                        g.memberIds(), d.sectionAuthors().stream().filter(GroupDocument.SectionAuthor::done)
                                .map(a -> new GroupSectionAuthor(a.partId(), a.accountId())).toList(),
                        d.submittedAt(), d.late(), d.receiptHash())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSubmissionStatus> listGroupSubmissions(UUID assignmentId, UUID classId) {
        Map<UUID, GroupDocument> byGroup = documents.listByAssignmentAndClass(assignmentId, classId).stream()
                .collect(Collectors.toMap(GroupDocument::groupId, Function.identity(), (a, b) -> a));
        return groups.listGroups(classId).stream().map(g -> {
            GroupDocument d = byGroup.get(g.id());
            return new GroupSubmissionStatus(d == null ? null : d.id(), g.id(), g.name(), d != null && d.submitted(),
                    d == null ? null : d.submittedAt(), d != null && d.late());
        }).toList();
    }

    /** Bài nhóm của lớp cho tab Students (khác biệt 7 của plan: lọc bài {@code GROUP} từ U07). */
    @Transactional(readOnly = true)
    public List<GroupAssignment> groupAssignments(Actor actor, UUID classId) {
        access.requireTeacher(actor, classId);
        return assignments.listForClass(classId).stream().filter(a -> a.submissionMode() == SubmissionMode.GROUP)
                .map(a -> new GroupAssignment(a.assignmentId(), a.title(), a.status().name(), a.opensAt(), a.closesAt()))
                .toList();
    }

    /** Tiến độ từng nhóm của một bài nhóm cho giảng viên chính của lớp (BR-U12-52). */
    @Transactional(readOnly = true)
    public List<GroupProgress> progress(Actor actor, UUID classId, UUID assignmentId) {
        access.requireTeacher(actor, classId);
        Map<UUID, GroupDocument> byGroup = documents.listByAssignmentAndClass(assignmentId, classId).stream()
                .collect(Collectors.toMap(GroupDocument::groupId, Function.identity(), (a, b) -> a));
        List<GroupProgress> result = new ArrayList<>();
        for (ClassGroup g : groups.listGroups(classId)) {
            GroupDocument d = byGroup.get(g.id());
            List<SectionProgress> sections = d == null ? List.of() : documents.listSections(d.id()).stream()
                    .map(s -> new SectionProgress(s.id(), s.title(), s.status(), s.claimedBy())).toList();
            long done = sections.stream().filter(s -> s.status() == GroupSection.Status.DONE).count();
            long claimed = sections.stream().filter(s -> s.status() == GroupSection.Status.CLAIMED).count();
            result.add(new GroupProgress(g.id(), g.name(), g.leaderId(), d == null ? null : d.id(),
                    d == null ? null : d.status().name(), (int) done, (int) claimed, (int) (sections.size() - done - claimed),
                    sections, d != null && d.submitted(), d == null ? null : d.submittedAt(), d != null && d.late(),
                    d == null || d.submitMode() == null ? null : d.submitMode().name()));
        }
        return result;
    }

    /** Sự kiện rời trang của thành viên khi bài bật theo dõi (BR-U12-38). */
    @Transactional
    public void recordActivity(Actor actor, UUID documentId, List<ActivityEventInput> events) {
        GroupDocContext ctx = docAccess.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        if (ctx.document().status() != GroupDocument.Status.IN_PROGRESS || !ctx.document().activityMonitoring()) {
            throw ApiException.conflict("GROUP_DOC_ACTIVITY_OFF", "Tài liệu không ghi nhận hoạt động");
        }
        if (events == null || events.isEmpty()) {
            return;
        }
        if (events.size() > MAX_ACTIVITY_EVENTS) {
            throw ApiException.payloadTooLarge("GROUP_DOC_ACTIVITY_TOO_MANY", "Quá nhiều sự kiện trong một lần gửi");
        }
        activity.record(actor, new WorkRef(WorkKind.GROUP_DOCUMENT, documentId, ctx.document().classId()), events);
    }

    public record GroupAssignment(UUID assignmentId, String title, String status, Instant opensAt, Instant closesAt) {
    }

    public record SectionProgress(UUID id, String title, GroupSection.Status status, UUID holderId) {
    }

    public record GroupProgress(UUID groupId, String groupName, UUID leaderId, UUID documentId, String documentStatus,
                                int done, int claimed, int open, List<SectionProgress> sections, boolean submitted,
                                Instant submittedAt, boolean late, String submitMode) {
    }
}
