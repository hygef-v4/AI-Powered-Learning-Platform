// Kiểm sơ bộ phía client theo `purpose` (BR-U03-02, 03). Backend vẫn kiểm lại theo nội dung tệp.

export type ArtifactPurpose = "AVATAR" | "MATERIAL" | "DOCUMENT_IMAGE";

const MB = 1024 * 1024;

export const PURPOSE_RULES: Record<ArtifactPurpose, { extensions: string[]; maxBytes: number; accept: string }> = {
  AVATAR: { extensions: ["jpg", "jpeg", "png", "webp"], maxBytes: 50 * MB, accept: ".jpg,.jpeg,.png,.webp" },
  MATERIAL: { extensions: ["pdf", "docx", "pptx"], maxBytes: 50 * MB, accept: ".pdf,.docx,.pptx" },
  DOCUMENT_IMAGE: { extensions: ["png", "jpg", "jpeg", "gif", "svg"], maxBytes: 5 * MB, accept: ".png,.jpg,.jpeg,.gif,.svg" },
};

/** Trả thông điệp lỗi tiếng Việt, hoặc `null` nếu tệp qua được kiểm sơ bộ. */
export function precheckFile(file: File, purpose: ArtifactPurpose): string | null {
  const rule = PURPOSE_RULES[purpose];
  const ext = file.name.includes(".") ? file.name.split(".").pop()!.toLowerCase() : "";
  if (!rule.extensions.includes(ext)) {
    return `Chỉ nhận tệp ${rule.extensions.map((e) => "." + e).join(", ")}.`;
  }
  if (file.size > rule.maxBytes) {
    return `Tệp vượt quá ${rule.maxBytes / MB} MB.`;
  }
  return null;
}
