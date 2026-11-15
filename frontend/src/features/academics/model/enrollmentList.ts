import { isValidStudentCode, normalizeStudentCode } from "@/shared/studentCode";

/** `email` giữ tên cũ: là giá trị đã chuẩn hóa của dòng, email hoặc MSSV (BR-U05-56, 58). */
export type EnrollmentPreviewRow = { line: number; email: string; error: "INVALID_IDENTIFIER" | "DUPLICATE_IN_LIST" | null };
const MAX_BYTES = 100 * 1024;
const EMAIL_PATTERN = /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/;
const HEADERS = new Set(["email", "student_code", "mssv"]);

/** Kiểm trước khi gửi: mỗi dòng một email hoặc MSSV. Tài khoản/role/quyền và kết quả ghi danh phải do server kiểm lại. */
export function parseEnrollmentList(input: string): EnrollmentPreviewRow[] {
  if (new TextEncoder().encode(input).length > MAX_BYTES) throw new Error("Danh sách tối đa 100 KB.");
  const rows: EnrollmentPreviewRow[] = [];
  const seen = new Set<string>();
  let first = true;
  input.replace(/^﻿/, "").split(/\r\n|\n|\r/).forEach((line, index) => {
    let value = line.trim();
    if (!value) return;
    if (value.startsWith('"') && value.endsWith('"') && value.length > 1) value = value.slice(1, -1).replaceAll('""', '"').trim();
    if (first && HEADERS.has(value.toLowerCase())) { first = false; return; }
    first = false;
    if (rows.length >= 200) throw new Error("Danh sách tối đa 200 dòng.");
    const code = normalizeStudentCode(value);
    const identifier = !value.includes("@") && isValidStudentCode(code) ? code : value.toLowerCase();
    const valid = identifier === code || (identifier.length <= 254 && EMAIL_PATTERN.test(identifier));
    const error = !valid ? "INVALID_IDENTIFIER" : seen.has(identifier) ? "DUPLICATE_IN_LIST" : null;
    if (!error) seen.add(identifier);
    rows.push({ line: index + 1, email: identifier, error });
  });
  if (!rows.length) throw new Error("Vui lòng nhập ít nhất một email hoặc MSSV.");
  return rows;
}
