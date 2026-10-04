package edu.aiplatform.academics.port;

import java.util.UUID;

public record LessonView(UUID lessonId, String title, String sourceType, boolean subjectMaterial, String mimeType, String youtubeUrl, int orderNo) {}
