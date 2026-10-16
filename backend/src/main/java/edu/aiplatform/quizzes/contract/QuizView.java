package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuizView(UUID quizId, QuizScope scope, UUID classId, UUID subjectId, int versionNo, String title,
                       QuizStatus status, QuizSettings settings, boolean activityMonitoringEnabled,
                       String activityNotice, List<UUID> referenceMaterialIds, BigDecimal maxScore) {
}
