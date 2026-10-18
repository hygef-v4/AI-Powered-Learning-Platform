export type EnrollmentPreviewRow = { line: number; email: string; error: "INVALID_EMAIL" | "DUPLICATE_IN_LIST" | null };
const MAX_BYTES = 100 * 1024;
const emailPattern = /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/;
/** Kiểm trước khi gửi. Tài khoản/role/quyền và kết quả ghi danh phải do server kiểm lại. */
export function parseEnrollmentList(input: string): EnrollmentPreviewRow[] {
  if (new TextEncoder().encode(input).length > MAX_BYTES) throw new Error("Danh sách tối đa 100 KB.");
  const rows: EnrollmentPreviewRow[] = [];
  const seen = new Set<string>();
  let first = true;
  input.replace(/^\uFEFF/, "").split(/\r\n|\n|\r/).forEach((line, index) => {
    let value = line.trim();
    if (!value) return;
    if (value.startsWith('"') && value.endsWith('"') && value.length > 1) value = value.slice(1, -1).replaceAll('""', '"').trim();
    if (first && value.toLowerCase() === "email") { first = false; return; }
    first = false;
    if (rows.length >= 200) throw new Error("Danh sách tối đa 200 dòng.");
    const email = value.toLowerCase();
    const error = email.length > 254 || !emailPattern.test(email) ? "INVALID_EMAIL" : seen.has(email) ? "DUPLICATE_IN_LIST" : null;
    if (!error) seen.add(email);
    rows.push({ line: index + 1, email, error });
  });
  if (!rows.length) throw new Error("Vui lòng nhập ít nhất một email.");
  return rows;
}
