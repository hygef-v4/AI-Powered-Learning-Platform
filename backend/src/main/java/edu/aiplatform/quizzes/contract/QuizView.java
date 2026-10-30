package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizView(UUID quizId, QuizScope scope, UUID classId, UUID subjectId, int versionNo, String title,
                       QuizStatus status, QuizSettings settings, boolean activityMonitoringEnabled,
                       String activityNotice, List<UUID> referenceMaterialIds, BigDecimal maxScore,
                       Instant publishedAt) {
    public QuizView(UUID quizId, QuizScope scope, UUID classId, UUID subjectId, int versionNo, String title,
                    QuizStatus status, QuizSettings settings, boolean activityMonitoringEnabled,
                    String activityNotice, List<UUID> referenceMaterialIds, BigDecimal maxScore) {
        this(quizId, scope, classId, subjectId, versionNo, title, status, settings, activityMonitoringEnabled,
                activityNotice, referenceMaterialIds, maxScore, null);
    }
}
