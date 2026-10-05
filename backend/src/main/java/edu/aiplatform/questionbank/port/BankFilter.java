package edu.aiplatform.questionbank.port;

import java.util.List;
import java.util.UUID;

public record BankFilter(AssignmentType assignmentType, QuestionType questionType, String difficulty, List<String> tags, UUID moduleId, UUID lessonId, String keyword) {}
