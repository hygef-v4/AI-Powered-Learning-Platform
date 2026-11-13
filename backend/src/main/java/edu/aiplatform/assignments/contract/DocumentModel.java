package edu.aiplatform.assignments.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Tài liệu dùng chung cho khung đề (U07), bài làm (U08) và tài liệu nhóm (U12). {@code title}: tên bài có định dạng của khung
 * đề ({@code {"runs": [...], "align": ...}}, yêu cầu 2026-11-04); chữ thuần của tên vẫn ở cột tên bài. Không có thì bỏ qua.
 */
public record DocumentModel(List<DocumentBlock> blocks, @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode title) {

    public DocumentModel(List<DocumentBlock> blocks) {
        this(blocks, null);
    }
}
