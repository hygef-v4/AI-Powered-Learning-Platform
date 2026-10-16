package edu.aiplatform.subjects.contract;

import java.util.UUID;

public record SubjectRef(UUID subjectId, String code, String name, SubjectStatus status, UUID managerId) {
}
