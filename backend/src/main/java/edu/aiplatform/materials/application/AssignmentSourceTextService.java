package edu.aiplatform.materials.application;

import edu.aiplatform.ai.contract.AssignmentSourceFile;
import edu.aiplatform.ai.contract.AssignmentSourceText;
import edu.aiplatform.ai.contract.IAssignmentSourceTextExtractor;
import edu.aiplatform.materials.infrastructure.MaterialTextExtractor;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Cầu nối U13: xác minh giới hạn nguồn và dùng parser tách biệt của học liệu. */
@Service
public class AssignmentSourceTextService implements IAssignmentSourceTextExtractor {

    private static final int MAX_FILES = 3;
    private static final long MAX_BYTES_PER_FILE = 2L * 1024 * 1024;
    private final MaterialTextExtractor extractor;

    public AssignmentSourceTextService(MaterialTextExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public List<AssignmentSourceText> extract(List<AssignmentSourceFile> files, int maxChars) {
        if (files == null || files.size() > MAX_FILES || maxChars < 1) {
            throw ApiException.badRequest("ASSIGNMENT_SOURCE_LIMIT", "Chọn tối đa 3 tệp nguồn");
        }
        int remaining = maxChars;
        List<AssignmentSourceText> result = new ArrayList<>();
        for (AssignmentSourceFile file : files) {
            if (file == null || file.fileId() == null || file.fileName() == null || file.byteSize() < 1
                    || file.byteSize() > MAX_BYTES_PER_FILE
                    || !List.of("text/plain", "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document").contains(file.mediaType())) {
                throw ApiException.badRequest("ASSIGNMENT_SOURCE_INVALID", "Một tệp nguồn không hợp lệ hoặc vượt 2 MB");
            }
            final String text;
            try {
                text = extractor.extract(file.fileId(), file.mediaType()).text();
            } catch (RuntimeException e) {
                throw ApiException.badRequest("ASSIGNMENT_SOURCE_UNREADABLE",
                        "Không đọc được " + file.fileName() + ". Dùng TXT UTF-8 hoặc PDF/DOCX có lớp chữ.");
            }
            if (text == null || text.isBlank()) {
                throw ApiException.badRequest("ASSIGNMENT_SOURCE_NO_TEXT",
                        "Không tìm thấy chữ trong " + file.fileName() + ". Tệp PDF scan chưa được hỗ trợ.");
            }
            int length = Math.min(remaining, text.length());
            result.add(new AssignmentSourceText(file.fileName(), text.substring(0, length), length < text.length()));
            remaining -= length;
        }
        return List.copyOf(result);
    }
}
