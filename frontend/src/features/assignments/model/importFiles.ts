import { checkFile, type PurposePolicy } from "@/features/files/model/purposes";
import { msg } from "@/i18n/msg";

/** Loại tệp nhập đề theo đuôi; máy chủ vẫn kiểm nội dung thật (BR-U07-88). */
export type ImportFileKind = "IMAGE" | "PDF" | "DOCX";

export const IMPORT_ACCEPT = ".png,.jpg,.jpeg,.webp,.pdf,.docx";
export const MAX_IMPORT_IMAGES = 5;
const DOCX_MAX_MB = 20;

export function importKindOf(file: { name: string }): ImportFileKind | null {
  const ext = file.name.includes(".") ? file.name.split(".").pop()!.toLowerCase() : "";
  if (["png", "jpg", "jpeg", "webp"].includes(ext)) return "IMAGE";
  if (ext === "pdf") return "PDF";
  if (ext === "docx") return "DOCX";
  return null;
}

/**
 * Kiểm danh sách tệp như máy chủ: tối đa 5 ảnh, hoặc đúng 1 PDF, hoặc đúng 1 DOCX; không trộn loại; dung lượng theo
 * chính sách {@code ASSIGNMENT_IMPORT} (DOCX theo giới hạn nhập DOCX). Trả câu lỗi đầu tiên, hợp lệ thì {@code null}.
 */
export function checkImportFiles(files: File[], policy: PurposePolicy | null): string | null {
  if (files.length === 0) return msg("assignments.model.importFiles.chonAnhPdfHoacDocxCua");
  const kinds = files.map(importKindOf);
  if (kinds.some((kind) => kind === null)) return msg("assignments.model.importFiles.chiNhanAnhPngJpegWebp");
  if (new Set(kinds).size > 1) return msg("assignments.model.importFiles.khongChonChungAnhVoiPdf");
  const kind = kinds[0];
  if (kind !== "IMAGE" && files.length > 1) return msg("assignments.model.importFiles.chiNhap1TepMoiLan", { kind: String(kind) });
  if (files.length > MAX_IMPORT_IMAGES) return msg("assignments.model.importFiles.chiNhapToiDaAnhMoi", { MAX_IMPORT_IMAGES: String(MAX_IMPORT_IMAGES) });
  for (const file of files) {
    if (kind === "DOCX") {
      if (file.size > DOCX_MAX_MB * 1024 * 1024) return msg("assignments.model.importFiles.tepDocxToiDaMb", { DOCX_MAX_MB: String(DOCX_MAX_MB) });
    } else if (policy) {
      const issue = checkFile(file, policy);
      if (issue) return `${file.name}: ${issue}`;
    }
  }
  return null;
}
