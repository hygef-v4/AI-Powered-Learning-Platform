package edu.aiplatform.assignmentwork.domain;

import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.GradingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Ảnh chụp bài lúc bắt đầu lượt (BR-U08-12): version, chế độ, hạn, sơ đồ bắt buộc, chính sách theo dõi hoạt động.
 * Bài đã phát hành không đổi nội dung; hạn có thể được kéo dài nên hạn nộp đọc lại từ U07 khi kiểm.
 */
public record AttemptSnapshot(int versionNo, String title, GradingMode gradingMode, Instant closesAt,
                              Instant lateUntil, Map<DiagramType, Integer> requiredDiagrams, boolean diagramEnabled,
                              boolean activityMonitoringEnabled, String activityNotice, int maxAttempts) {

    public AttemptSnapshot {
        // Sơ đồ luôn dùng được (yêu cầu 2026-10-21), kể cả lượt đã chụp khi bài còn tắt sơ đồ.
        diagramEnabled = true;
        requiredDiagrams = requiredDiagrams == null ? Map.of() : Map.copyOf(requiredDiagrams);
    }

    public static AttemptSnapshot of(AssignmentView view, Map<DiagramType, Integer> requiredDiagrams) {
        return new AttemptSnapshot(view.versionNo(), view.title(), view.gradingMode(), view.closesAt(),
                view.lateUntil(), requiredDiagrams, view.diagramEnabled(), view.activityMonitoringEnabled(),
                view.activityNotice(), view.maxAttempts() == null ? 1 : view.maxAttempts());
    }
}
