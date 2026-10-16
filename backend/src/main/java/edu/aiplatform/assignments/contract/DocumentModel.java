package edu.aiplatform.assignments.contract;

import java.util.List;

/** Tài liệu dùng chung cho khung đề (U07), bài làm (U08) và tài liệu nhóm (U12). */
public record DocumentModel(List<DocumentBlock> blocks) {
}
