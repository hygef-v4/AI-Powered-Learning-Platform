// Mã số sinh viên (BR-U01-100): 2 chữ cái và 6 chữ số, lưu chữ in hoa. Backend là nơi kiểm cuối cùng.

export const STUDENT_CODE_PATTERN = /^[A-Z]{2}[0-9]{6}$/;

export const STUDENT_CODE_FORMAT_MESSAGE = "MSSV gồm 2 chữ cái và 6 chữ số, ví dụ SE123456.";

/** Cắt khoảng trắng hai đầu và đổi chữ in hoa; giữ ký tự ở giữa để kiểm định dạng báo lỗi. */
export function normalizeStudentCode(raw: string) {
  return raw.trim().toUpperCase();
}

export function isValidStudentCode(code: string) {
  return STUDENT_CODE_PATTERN.test(code);
}

/** Câu báo cho lỗi MSSV từ backend (frontend-components U01 mục 6). */
export const STUDENT_CODE_ERROR_LABELS: Record<string, string> = {
  STUDENT_CODE_EXISTS: "MSSV này đã được dùng cho tài khoản khác.",
  STUDENT_CODE_LOCKED: "MSSV đã khóa vì sinh viên đã kích hoạt tài khoản.",
  MISSING_STUDENT_CODE: "Cần nhập MSSV cho tài khoản sinh viên.",
  STUDENT_CODE_NOT_ALLOWED: "Chỉ tài khoản sinh viên mới có MSSV.",
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
