package edu.aiplatform.files.port;

import edu.aiplatform.shared.contract.ActorRef;

/** Chữ ký theo thiết kế U01; U03 cài (F5). Trả `fileId` để U01 lưu `avatar_file_id`. */
public interface AvatarPort {
    String validateAvatar(FileRef fileRef, ActorRef actor);
}
