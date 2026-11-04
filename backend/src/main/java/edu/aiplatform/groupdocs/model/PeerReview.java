package edu.aiplatform.groupdocs.model;

import java.time.Instant;
import java.util.UUID;

/** Một thành viên chấm một thành viên khác trong cùng tài liệu nhóm (FB-U12-04); điểm 1–5. */
public record PeerReview(UUID id, UUID documentId, UUID reviewerId, UUID revieweeId, int score, String comment,
                         Instant createdAt, Instant updatedAt) {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;
    public static final int MAX_COMMENT = 1000;
}
