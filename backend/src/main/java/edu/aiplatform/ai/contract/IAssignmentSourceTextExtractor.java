package edu.aiplatform.ai.contract;

import java.util.List;

/** U13 trích chữ tệp nguồn qua parser cách ly; U07 chỉ nhận tối đa số ký tự được cấp. */
public interface IAssignmentSourceTextExtractor {

    List<AssignmentSourceText> extract(List<AssignmentSourceFile> files, int maxChars);
}
