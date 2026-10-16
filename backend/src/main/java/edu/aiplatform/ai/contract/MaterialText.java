package edu.aiplatform.ai.contract;

import java.util.UUID;

public record MaterialText(UUID materialId, String title, String text, boolean truncated) {
}
