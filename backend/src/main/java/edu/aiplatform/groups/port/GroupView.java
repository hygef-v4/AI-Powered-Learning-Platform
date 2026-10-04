package edu.aiplatform.groups.port;

import java.util.List;
import java.util.UUID;

public record GroupView(UUID groupId, UUID classId, String name, UUID leaderId, List<UUID> memberIds) {}
