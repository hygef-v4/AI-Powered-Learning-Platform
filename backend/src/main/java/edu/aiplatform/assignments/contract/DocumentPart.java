package edu.aiplatform.assignments.contract;

import java.util.List;
import java.util.UUID;

/** Phần tự tính từ heading nhỏ nhất của mỗi nhánh; {@code partId} là {@code DOCUMENT} khi khung không có heading. */
public record DocumentPart(String partId, UUID headingBlockId, List<UUID> ancestorHeadingIds, String title) {
}
