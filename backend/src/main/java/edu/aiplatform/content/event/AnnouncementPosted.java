package edu.aiplatform.content.event;

import java.util.UUID;

public record AnnouncementPosted(UUID announcementId, UUID actorId, UUID classId) {}
