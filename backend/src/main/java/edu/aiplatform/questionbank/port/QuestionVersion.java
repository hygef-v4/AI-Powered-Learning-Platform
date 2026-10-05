package edu.aiplatform.questionbank.port;

import java.math.BigDecimal;
import java.util.UUID;

/** Phiên bản bất biến; `definition` là JSON theo loại câu. */
public record QuestionVersion(UUID id, UUID lineageId, int version, QuestionType questionType, String title,
                              BigDecimal defaultPoints, String status, String definitionJson) {}
