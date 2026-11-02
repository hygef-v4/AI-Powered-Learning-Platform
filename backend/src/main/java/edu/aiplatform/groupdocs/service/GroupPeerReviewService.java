package edu.aiplatform.groupdocs.service;

import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.PeerReview;
import edu.aiplatform.groupdocs.repository.GroupPeerReviewRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đánh giá chéo giữa thành viên nhóm (BR-U12-47). Mở sau khi nhóm có bản nộp, sửa được tới 7 ngày sau hạn cuối
 * nhận bài (không có hạn thì không đóng). Thành viên chấm mỗi thành viên khác 1–5 kèm nhận xét và chỉ thấy đánh giá
 * của mình; giảng viên chính của lớp thấy trung bình, quy đổi thang 10, số người chấm và từng nhận xét. Điểm chỉ để
 * giảng viên tham khảo; ghép vào điểm cuối theo trọng số là việc của U09.
 */
@Service
public class GroupPeerReviewService {

    public static final Duration WINDOW_AFTER_DEADLINE = Duration.ofDays(7);

    private final GroupPeerReviewRepository reviews;
    private final GroupDocAccess access;
    private final IAssignmentQuery assignments;
    private final IAudit audit;
    private final Clock clock;

    public GroupPeerReviewService(GroupPeerReviewRepository reviews, GroupDocAccess access, IAssignmentQuery assignments,
                                  IAudit audit, Clock clock) {
        this.reviews = reviews;
        this.access = access;
        this.assignments = assignments;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PeerReviewView view(Actor actor, UUID documentId) {
        GroupDocContext ctx = access.read(actor, documentId);
        Window window = window(ctx);
        List<PeerReview> all = reviews.listByDocument(documentId);
        List<UUID> members = ctx.group().memberIds();
        if (ctx.teacher()) {
            List<MemberSummary> summary = new ArrayList<>();
            for (UUID member : members) {
                List<PeerReview> received = all.stream()
                        .filter(r -> r.revieweeId().equals(member) && members.contains(r.reviewerId())).toList();
                Double average = received.isEmpty() ? null
                        : received.stream().mapToInt(PeerReview::score).average().orElse(0);
                summary.add(new MemberSummary(member, average,
                        average == null ? null : Math.round(average / PeerReview.MAX_SCORE * 100) / 10.0,
                        received.size(), received.stream()
                        .map(r -> new Received(r.reviewerId(), r.score(), r.comment(), r.updatedAt())).toList()));
            }
            long reviewers = all.stream().map(PeerReview::reviewerId).filter(members::contains).distinct().count();
            return new PeerReviewView(window.open(), window.reason(), window.closesAt(), List.of(), summary,
                    (int) reviewers, members.size());
        }
        GroupDocAccess.requireMember(ctx);
        List<Given> mine = all.stream().filter(r -> r.reviewerId().equals(ctx.actorId()))
                .filter(r -> members.contains(r.revieweeId()))
                .map(r -> new Given(r.revieweeId(), r.score(), r.comment())).toList();
        return new PeerReviewView(window.open(), window.reason(), window.closesAt(), mine, List.of(), 0, members.size());
    }

    /** Thành viên lưu đánh giá cho các thành viên khác; gửi lại thì thay bản cũ. */
    @Transactional
    public PeerReviewView save(Actor actor, UUID documentId, List<Given> given) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        Window window = window(ctx);
        if (!window.open()) {
            throw ApiException.conflict("PEER_REVIEW_CLOSED", window.reason());
        }
        List<Given> items = given == null ? List.of() : given;
        Set<UUID> seen = new HashSet<>();
        for (Given g : items) {
            if (g.revieweeId() == null || g.revieweeId().equals(ctx.actorId()) || !ctx.group().isMember(g.revieweeId())
                    || !seen.add(g.revieweeId())) {
                throw ApiException.badRequest("PEER_REVIEW_INVALID", "Chỉ đánh giá mỗi thành viên khác trong nhóm một lần");
            }
            if (g.score() < PeerReview.MIN_SCORE || g.score() > PeerReview.MAX_SCORE) {
                throw ApiException.badRequest("PEER_REVIEW_INVALID", "Điểm đánh giá từ 1 đến 5");
            }
            if (g.comment() != null && g.comment().length() > PeerReview.MAX_COMMENT) {
                throw ApiException.badRequest("PEER_REVIEW_INVALID", "Nhận xét tối đa 1000 ký tự");
            }
        }
        Instant now = clock.instant();
        for (Given g : items) {
            String comment = g.comment() == null || g.comment().isBlank() ? null : g.comment().trim();
            reviews.upsert(new PeerReview(UUID.randomUUID(), documentId, ctx.actorId(), g.revieweeId(), g.score(),
                    comment, now, now));
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reviewees", items.size());
        audit.record(new AuditEvent(UUID.randomUUID(), ctx.actorId(), "GROUP_PEER_REVIEW_SAVED", "GROUP_DOCUMENT",
                documentId.toString(), AuditResult.SUCCESS, null, details, now));
        return view(actor, documentId);
    }

    private Window window(GroupDocContext ctx) {
        GroupDocument document = ctx.document();
        AssignmentView assignment = assignments.get(document.assignmentId()).orElse(null);
        Instant deadline = assignment == null ? null
                : assignment.lateUntil() != null ? assignment.lateUntil() : assignment.closesAt();
        Instant closesAt = deadline == null ? null : deadline.plus(WINDOW_AFTER_DEADLINE);
        if (document.submittedAt() == null) {
            return new Window(false, "Mở sau khi nhóm nộp bài", closesAt);
        }
        if (closesAt != null && !clock.instant().isBefore(closesAt)) {
            return new Window(false, "Đã hết thời gian đánh giá", closesAt);
        }
        return new Window(true, null, closesAt);
    }

    private record Window(boolean open, String reason, Instant closesAt) {
    }

    /** Đánh giá một thành viên gửi cho một thành viên khác. */
    public record Given(UUID revieweeId, int score, String comment) {
    }

    public record Received(UUID reviewerId, int score, String comment, Instant updatedAt) {
    }

    /** {@code outOfTen}: trung bình quy đổi thang 10, làm tròn 1 chữ số. */
    public record MemberSummary(UUID accountId, Double average, Double outOfTen, int count, List<Received> reviews) {
    }

    /**
     * Thành viên nhận {@code mine}; giảng viên nhận {@code summary} cùng số thành viên đã đánh giá ({@code reviewers})
     * trên tổng số thành viên.
     */
    public record PeerReviewView(boolean open, String closedReason, Instant closesAt, List<Given> mine,
                                 List<MemberSummary> summary, int reviewers, int members) {
    }
}
