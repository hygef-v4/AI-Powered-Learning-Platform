package edu.aiplatform.groupdocs.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.SectionRules;
import edu.aiplatform.groups.application.GroupAccess;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền với tài liệu nhóm (BR-U12-31): thành viên hiệu lực của nhóm hoặc giảng viên chính của lớp; nhóm khác,
 * người ngoài lớp, Admin nhận {@code 404}. Kiểm tài liệu còn sửa được (BR-U12-32).
 */
@Component
public class GroupDocAccess {

    private final GroupDocumentRepository documents;
    private final GroupRepository groups;
    private final GroupAccess access;
    private final IAssignmentQuery assignments;
    private final Clock clock;

    public GroupDocAccess(GroupDocumentRepository documents, GroupRepository groups, GroupAccess access,
                          IAssignmentQuery assignments, Clock clock) {
        this.documents = documents;
        this.groups = groups;
        this.access = access;
        this.assignments = assignments;
        this.clock = clock;
    }

    /** Thành viên hoặc giảng viên của lớp xem tài liệu. */
    public GroupDocContext read(Actor actor, UUID documentId) {
        GroupDocument document = documents.findById(documentId).orElseThrow(GroupAccess::notFound);
        return context(actor, document);
    }

    /** Như {@link #read} nhưng khóa dòng tài liệu (nộp, tự nộp). */
    public GroupDocContext lock(Actor actor, UUID documentId) {
        GroupDocument document = documents.lockDocument(documentId).orElseThrow(GroupAccess::notFound);
        return context(actor, document);
    }

    public GroupDocContext context(Actor actor, GroupDocument document) {
        ClassGroup group = groups.findGroup(document.groupId()).orElseThrow(GroupAccess::notFound);
        if (actor != null && actor.role() == Role.STUDENT) {
            ClassRef ref = access.requireStudent(actor, document.classId());
            if (!group.isMember(actor.accountId())) {
                throw GroupAccess.notFound();
            }
            return new GroupDocContext(document, group, ref, actor.accountId(), true,
                    actor.accountId().equals(group.leaderId()), false);
        }
        ClassRef ref = access.requireTeacher(actor, document.classId());
        return new GroupDocContext(document, group, ref, actor.accountId(), false, false, true);
    }

    /** Thành viên mới được sửa (giảng viên chỉ xem và nhả khóa). */
    public static void requireMember(GroupDocContext ctx) {
        if (!ctx.member()) {
            throw ApiException.forbidden("GROUP_DOC_MEMBER_ONLY");
        }
    }

    /** Tài liệu {@code IN_PROGRESS} và bài còn nhận bài; {@code withGrace} cho lần lưu nháp cuối (BR-U12-32). */
    public void requireEditable(GroupDocContext ctx, boolean withGrace) {
        if (ctx.document().status() != GroupDocument.Status.IN_PROGRESS) {
            throw ApiException.conflict("GROUP_DOC_CLOSED", "Tài liệu đã đóng, không sửa được nữa");
        }
        if (window(ctx.document().assignmentId(), withGrace) == SubmissionWindow.CLOSED) {
            throw ApiException.conflict("GROUP_DOC_DEADLINE_PASSED", "Đã hết hạn nộp bài");
        }
    }

    public SubmissionWindow window(UUID assignmentId, boolean withGrace) {
        Instant now = clock.instant();
        try {
            SubmissionWindow window = assignments.submissionWindow(assignmentId, now);
            // Ân hạn chỉ nới hạn đóng; lùi giờ ngay từ đầu thì 30 giây đầu sau khi mở bài bị coi là chưa mở.
            if (window == SubmissionWindow.CLOSED && withGrace) {
                window = assignments.submissionWindow(assignmentId, now.minus(SectionRules.GRACE));
            }
            return window;
        } catch (RuntimeException e) {
            throw ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
        }
    }
}
