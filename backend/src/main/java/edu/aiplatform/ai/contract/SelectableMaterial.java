package edu.aiplatform.ai.contract;

import java.util.UUID;

/** {@code video}: học liệu là link video; Quiz không đưa loại này vào nguồn cho AI (yêu cầu 2026-11-27). */
public record SelectableMaterial(UUID materialId, String title, boolean hasText, boolean video) {

    public SelectableMaterial(UUID materialId, String title, boolean hasText) {
        this(materialId, title, hasText, false);
    }
}
