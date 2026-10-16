package edu.aiplatform.ai.contract;

import java.util.UUID;

public record SelectableMaterial(UUID materialId, String title, boolean hasText) {
}
