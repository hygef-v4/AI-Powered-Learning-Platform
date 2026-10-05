package edu.aiplatform.questionbank.port;

import java.util.UUID;

/** U10: sao câu ngân hàng cấp lớp sang lớp đích (BR-U10-13). */
public interface BankCopyPort {
    QuestionVersion copyToClass(UUID questionId, UUID targetClassId);
}
