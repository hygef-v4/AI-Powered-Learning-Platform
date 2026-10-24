package edu.aiplatform.files.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Loại tệp được phép, nhận dạng theo nội dung (BR-U03-03). */
public enum FileType {
    PDF("application/pdf", List.of("pdf")),
    TXT("text/plain", List.of("txt")),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", List.of("docx")),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", List.of("pptx")),
    PNG("image/png", List.of("png")),
    JPEG("image/jpeg", List.of("jpg", "jpeg")),
    GIF("image/gif", List.of("gif")),
    SVG("image/svg+xml", List.of("svg"));

    private final String mediaType;
    private final List<String> extensions;

    FileType(String mediaType, List<String> extensions) {
        this.mediaType = mediaType;
        this.extensions = extensions;
    }

    public String mediaType() {
        return mediaType;
    }

    public List<String> extensions() {
        return extensions;
    }

    public boolean isImage() {
        return mediaType.startsWith("image/");
    }

    /** Ảnh và PDF mở trong trình duyệt; loại khác luôn tải xuống (BR-U03-23). */
    public boolean inline() {
        return isImage() || this == PDF;
    }

    public static Optional<FileType> fromMediaType(String mediaType) {
        return Arrays.stream(values()).filter(type -> type.mediaType.equals(mediaType)).findFirst();
    }
}
