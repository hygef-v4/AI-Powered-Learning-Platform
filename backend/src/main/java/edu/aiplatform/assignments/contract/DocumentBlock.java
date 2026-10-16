package edu.aiplatform.assignments.contract;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/** Nội dung theo {@code type} (domain-entities U07 mục 3): heading, runs, items, rows, ảnh, sơ đồ Draw.io. */
public record DocumentBlock(UUID id, BlockOrigin origin, BlockType type, JsonNode content) {
}
