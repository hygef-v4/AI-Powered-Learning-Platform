package edu.aiplatform.academics.port;

import java.util.List;
import java.util.UUID;

public record ModuleView(UUID moduleId, String title, int orderNo, List<LessonView> lessons) {}
