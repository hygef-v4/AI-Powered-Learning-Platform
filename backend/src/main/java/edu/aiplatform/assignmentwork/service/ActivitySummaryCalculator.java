package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignmentwork.contract.ActivityInterval;
import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IntervalCertainty;
import edu.aiplatform.assignmentwork.model.ActivityEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tóm tắt rời trang (BR-U08-53; P4): ghép {@code HIDDEN} với {@code VISIBLE} theo phiên thành khoảng rời đã biết;
 * khoảng không có sự kiện quá 2 phút, sự kiện thiếu số thứ tự, giờ client lệch bất thường, rời trang chưa thấy quay
 * lại là "không rõ". Chỉ là dấu hiệu, không kết luận gian lận.
 */
final class ActivitySummaryCalculator {

    static final Duration HEARTBEAT_GAP = Duration.ofMinutes(2);
    static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    static final String NOTE = "Dữ liệu chỉ là dấu hiệu rời trang, không kết luận gian lận.";

    private ActivitySummaryCalculator() {
    }

    static ActivitySummary summarize(List<ActivityEvent> events) {
        if (events.isEmpty()) {
            return new ActivitySummary(ActivityPolicyState.NO_DATA, 0, Duration.ZERO, List.of(), 0,
                    "Chưa có dữ liệu. " + NOTE);
        }
        Map<UUID, List<ActivityEvent>> sessions = new LinkedHashMap<>();
        events.stream().sorted(Comparator.comparing(ActivityEvent::serverTime))
                .forEach(e -> sessions.computeIfAbsent(e.sessionId(), id -> new ArrayList<>()).add(e));
        List<ActivityInterval> intervals = new ArrayList<>();
        int away = 0;
        int unknown = 0;
        Duration known = Duration.ZERO;
        Instant previousEnd = null;
        List<List<ActivityEvent>> ordered = new ArrayList<>(sessions.values());
        ordered.sort(Comparator.comparing(list -> list.get(0).serverTime()));
        for (List<ActivityEvent> session : ordered) {
            session.sort(Comparator.comparingLong(ActivityEvent::sequence));
            Duration offset = offset(session);
            Instant first = time(session.get(0), offset);
            if (previousEnd != null && Duration.between(previousEnd, first).compareTo(HEARTBEAT_GAP) > 0) {
                intervals.add(new ActivityInterval(previousEnd, first, IntervalCertainty.UNKNOWN));
                unknown++;
            }
            boolean visible = true;
            Instant awayStart = null;
            Instant last = null;
            long lastSequence = -1;
            for (ActivityEvent event : session) {
                Instant at = time(event, offset);
                if (event.clientTime() != null && Duration.between(event.serverTime(), at).abs().compareTo(CLOCK_SKEW) > 0) {
                    at = event.serverTime();
                    unknown++;
                }
                if (lastSequence >= 0 && event.sequence() > lastSequence + 1) {
                    unknown++;
                }
                if (last != null && visible && Duration.between(last, at).compareTo(HEARTBEAT_GAP) > 0) {
                    intervals.add(new ActivityInterval(last, at, IntervalCertainty.UNKNOWN));
                    unknown++;
                }
                if (event.kind() == ActivityKind.HIDDEN && visible) {
                    visible = false;
                    awayStart = at;
                } else if ((event.kind() == ActivityKind.VISIBLE || event.kind() == ActivityKind.SESSION_END
                        || event.kind() == ActivityKind.SESSION_START) && !visible) {
                    intervals.add(new ActivityInterval(awayStart, at, IntervalCertainty.KNOWN));
                    away++;
                    known = known.plus(Duration.between(awayStart, at).isNegative() ? Duration.ZERO
                            : Duration.between(awayStart, at));
                    visible = true;
                    awayStart = null;
                }
                last = at;
                lastSequence = Math.max(lastSequence, event.sequence());
            }
            if (!visible && awayStart != null) {
                intervals.add(new ActivityInterval(awayStart, last, IntervalCertainty.UNKNOWN));
                away++;
                unknown++;
            }
            previousEnd = last;
        }
        String note = unknown == 0 ? NOTE
                : "Có " + unknown + " chỗ thiếu hoặc lệch dữ liệu, ghi là không rõ. " + NOTE;
        return new ActivitySummary(ActivityPolicyState.AVAILABLE, away, known, List.copyOf(intervals), unknown, note);
    }

    /** Lệch giữa đồng hồ server và client của phiên, lấy ở sự kiện đầu có giờ client. */
    private static Duration offset(List<ActivityEvent> session) {
        for (ActivityEvent event : session) {
            if (event.clientTime() != null) {
                return Duration.between(event.clientTime(), event.serverTime());
            }
        }
        return Duration.ZERO;
    }

    private static Instant time(ActivityEvent event, Duration offset) {
        return event.clientTime() == null ? event.serverTime() : event.clientTime().plus(offset);
    }
}
