package edu.aiplatform.quizzes.domain;

/** {@code PENDING → READY | FAILED}; {@code READY → ACCEPTED}; mọi trạng thái chưa chốt → {@code DISCARDED}. */
public enum DraftStatus {
    PENDING, READY, ACCEPTED, DISCARDED, FAILED;

    public boolean isOpen() {
        return this == PENDING || this == READY || this == FAILED;
    }
}
