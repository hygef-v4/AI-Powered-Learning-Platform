package edu.aiplatform.questionbank.port;

import java.util.UUID;

/** scopeType: SUBJECT, CLASS hoặc ASSIGNMENT (câu riêng của bài). */
public record BankScope(String scopeType, UUID scopeId) {}
