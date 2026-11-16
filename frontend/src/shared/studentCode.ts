import { msg } from "@/i18n/msg";

// Mã số sinh viên (BR-U01-100): 2 chữ cái và 6 chữ số, lưu chữ in hoa. Backend là nơi kiểm cuối cùng.

export const STUDENT_CODE_PATTERN = /^[A-Z]{2}[0-9]{6}$/;

export const STUDENT_CODE_FORMAT_MESSAGE = msg("shared.studentCode.mssvGom2ChuCaiVa");

/** Cắt khoảng trắng hai đầu và đổi chữ in hoa; giữ ký tự ở giữa để kiểm định dạng báo lỗi. */
export function normalizeStudentCode(raw: string) {
  return raw.trim().toUpperCase();
}

export function isValidStudentCode(code: string) {
  return STUDENT_CODE_PATTERN.test(code);
}

/** Câu báo cho lỗi MSSV từ backend (frontend-components U01 mục 6). */
export const STUDENT_CODE_ERROR_LABELS: Record<string, string> = {
  STUDENT_CODE_EXISTS: msg("shared.studentCode.mssvNayDaDuocDungCho"),
  STUDENT_CODE_LOCKED: msg("shared.studentCode.mssvDaKhoaViSinhVien"),
  MISSING_STUDENT_CODE: msg("shared.studentCode.canNhapMssvChoTaiKhoan"),
  STUDENT_CODE_NOT_ALLOWED: msg("shared.studentCode.chiTaiKhoanSinhVienMoi"),
  INVALID_STUDENT_CODE: STUDENT_CODE_FORMAT_MESSAGE,
};

/** So khớp ô tìm theo tên, email hoặc MSSV, không phân biệt hoa thường. */
export function matchesPersonQuery(
  person: { name?: string | null; email?: string | null; studentCode?: string | null },
  query: string,
) {
  const needle = query.trim().toLocaleLowerCase("vi");
  if (!needle) return true;
  return [person.name, person.email, person.studentCode]
    .some((value) => (value ?? "").toLocaleLowerCase("vi").includes(needle));
}
