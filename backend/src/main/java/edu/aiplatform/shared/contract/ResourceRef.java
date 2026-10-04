package edu.aiplatform.shared.contract;

import java.util.UUID;

/** Đối tượng cần kiểm quyền, ví dụ ("CLASS", classId). */
public record ResourceRef(String type, UUID id) {}
