package edu.aiplatform.questionbank.port;

import java.util.UUID;

/** U06 khai báo, U08 cài (`C`): bài `DRAFT` chuyển sang phiên bản rubric mới (BR-U06-36). */
public interface RubricOwnerPort {
    void repoint(UUID oldRubricId, UUID newRubricId);
}
