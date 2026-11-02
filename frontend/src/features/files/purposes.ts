export type ArtifactPurpose = "MATERIAL" | "DOCUMENT_IMAGE" | "ASSIGNMENT_SOURCE";

export type PurposePolicy = {
  purpose: ArtifactPurpose;
  maxSizeMb: number;
  allowedTypes: string[];
  extensions: string[];
  mediaTypes: string[];
};

export type UploadResult = {
  fileRef: { value: string };
  mediaType: string;
  byteSize: number;
  originalFileName: string;
  expiresAt: string;
};

/** Thuộc tính accept cho ô chọn tệp. */
export function acceptAttribute(policy: PurposePolicy): string {
  return policy.extensions.map((ext) => `.${ext}`).join(",");
}

/** Kiểm sơ bộ đuôi và dung lượng; backend vẫn kiểm nội dung thật (BR-U03-02, 03). */
export function checkFile(file: { name: string; size: number }, policy: PurposePolicy): string | null {
  const ext = file.name.includes(".") ? file.name.split(".").pop()!.toLowerCase() : "";
  if (!policy.extensions.includes(ext)) {
    return `Chỉ nhận tệp ${policy.allowedTypes.join(", ")}`;
  }
  if (file.size > policy.maxSizeMb * 1024 * 1024) {
    return `Tệp vượt ${policy.maxSizeMb} MB`;
  }
  return null;
}
