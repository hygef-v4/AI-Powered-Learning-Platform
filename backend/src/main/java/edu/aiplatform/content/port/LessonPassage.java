package edu.aiplatform.content.port;

import java.util.UUID;

public record LessonPassage(UUID lessonId, String title, String excerpt, double score) {}
