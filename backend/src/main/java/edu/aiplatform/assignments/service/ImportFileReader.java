package edu.aiplatform.assignments.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Kiểm tệp đề cũ trước khi đọc (BR-U07-88): số tệp, loại theo nội dung và dung lượng qua U03 (mục đích
 * {@code ASSIGNMENT_IMPORT}), không trộn ảnh với PDF. Mọi lỗi xảy ra trước khi tạo bản nháp hay gọi AI.
 */
@Component
public class ImportFileReader {

    static final int MAX_IMAGES = 5;
    static final String PDF = "application/pdf";

    private final IArtifact artifacts;

    public ImportFileReader(IArtifact artifacts) {
        this.artifacts = artifacts;
    }

    /** Tệp người dùng gửi, theo thứ tự đã sắp. */
    public record ImportFile(String fileName, byte[] content) {
    }

    /** Tệp đã kiểm, kèm media type nhận diện theo nội dung. */
    public record CheckedFile(String fileName, String mediaType, byte[] content) {
    }

    public enum FileKind {
        IMAGE, PDF
    }

    public record CheckedFiles(FileKind kind, List<CheckedFile> files, long totalBytes) {
    }

    public CheckedFiles readFiles(Actor actor, List<ImportFile> files) {
        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("ASSIGNMENT_IMPORT_FILE_INVALID", "Chọn ít nhất một ảnh hoặc PDF đề cũ");
        }
        if (files.size() > MAX_IMAGES) {
            throw ApiException.badRequest("ASSIGNMENT_IMPORT_TOO_MANY_FILES", "Chỉ nhập tối đa 5 ảnh mỗi lần");
        }
        List<CheckedFile> checked = new ArrayList<>();
        long total = 0;
        for (ImportFile file : files) {
            byte[] content = file.content() == null ? new byte[0] : file.content();
            checked.add(new CheckedFile(file.fileName(), inspectFile(actor, file.fileName(), content), content));
            total += content.length;
        }
        long pdfs = checked.stream().filter(f -> PDF.equals(f.mediaType())).count();
        if (pdfs > 0 && pdfs < checked.size()) {
            throw ApiException.badRequest("ASSIGNMENT_IMPORT_MIXED", "Không chọn chung ảnh với PDF trong một lần nhập");
        }
        if (pdfs > 1) {
            throw ApiException.badRequest("ASSIGNMENT_IMPORT_TOO_MANY_FILES", "Chỉ nhập 1 PDF mỗi lần");
        }
        return new CheckedFiles(pdfs == 1 ? FileKind.PDF : FileKind.IMAGE, List.copyOf(checked), total);
    }

    private String inspectFile(Actor actor, String fileName, byte[] content) {
        try {
            return artifacts.inspect(actor, ArtifactPurpose.ASSIGNMENT_IMPORT, fileName, content);
        } catch (ApiException e) {
            throw switch (e.code()) {
                case "FILE_TOO_LARGE" -> ApiException.badRequest("ASSIGNMENT_IMPORT_TOO_LARGE", e.getMessage());
                case "FILE_TYPE_NOT_ALLOWED" -> ApiException.badRequest("ASSIGNMENT_IMPORT_FILE_INVALID",
                        "Chỉ nhận ảnh PNG, JPEG, WEBP hoặc PDF");
                default -> e;
            };
        }
    }
}
