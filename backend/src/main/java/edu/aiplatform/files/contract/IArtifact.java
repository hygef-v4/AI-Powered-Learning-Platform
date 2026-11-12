package edu.aiplatform.files.contract;

import edu.aiplatform.identity.contract.Actor;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Lưu tệp riêng tư trên Drive, gắn tệp vào đối tượng của unit sở hữu, cấp link tải sau khi unit sở hữu
 * đã kiểm quyền, đọc tệp phía server. Dùng bởi U07, U08, U12, U13.
 */
public interface IArtifact {

    FileRef store(Actor actor, ArtifactPurpose purpose, FileUpload file);

    StoredFileInfo attach(FileRef fileRef, Actor actor, ArtifactPurpose purpose);

    DownloadToken issueDownloadToken(String fileId, UUID accountId);

    InputStream open(String fileId);

    /**
     * Link tải 5 phút để chính chủ xem lại tệp vừa lưu mà chưa gắn (ví dụ hình cắt cho bản xem trước nhập đề), không
     * đổi trạng thái tệp. Rỗng khi {@code fileRef} sai chữ ký, hết hạn hoặc không thuộc {@code actor} (BR-U03-101).
     */
    Optional<DownloadToken> issuePreviewToken(FileRef fileRef, Actor actor);

    /**
     * Kiểm tệp theo {@code purpose} (quyền, dung lượng, loại theo nội dung) mà không lưu, trả media type nhận diện
     * được. Dùng cho mục đích chỉ kiểm như {@link ArtifactPurpose#ASSIGNMENT_IMPORT} (BR-U03-99).
     */
    String inspect(Actor actor, ArtifactPurpose purpose, String originalFileName, byte[] content);
}
