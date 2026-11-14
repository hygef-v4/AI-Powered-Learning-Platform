/** Đọc sheet đầu của mẫu Excel (một cột email hoặc MSSV); server vẫn kiểm lại từng dòng và quyền ghi danh. */
const HEADERS = new Set(["email", "student_code", "mssv"]);

export async function readEnrollmentWorkbook(file: File): Promise<string> {
  if (file.size > 100 * 1024) throw new Error("Danh sách tối đa 100 KB.");
  if (!file.name.toLowerCase().endsWith(".xlsx")) throw new Error("Chỉ nhận tệp Excel .xlsx.");
  const bytes = new Uint8Array(await file.arrayBuffer());
  if (bytes[0] !== 0x50 || bytes[1] !== 0x4b) throw new Error("Không phải tệp Excel .xlsx.");
  const XLSX = await import("xlsx");
  let workbook;
  try {
    workbook = XLSX.read(bytes, { type: "array", sheetRows: 202 });
  } catch {
    throw new Error("Không đọc được tệp Excel .xlsx.");
  }
  const sheet = workbook.Sheets[workbook.SheetNames[0] ?? ""];
  if (!sheet?.["!ref"]) throw new Error("Tệp Excel trống.");
  const range = XLSX.utils.decode_range(sheet["!fullref"] ?? sheet["!ref"]);
  if (range.e.r > 200) throw new Error("Danh sách tối đa 200 dòng.");
  if (range.e.c > 0) throw new Error("Tệp Excel chỉ được có một cột email hoặc MSSV theo mẫu.");
  const header = String(sheet.A1?.v ?? "").trim().toLowerCase();
  if (sheet.A1?.f || !HEADERS.has(header)) {
    throw new Error("Tệp Excel cần cột email (hoặc student_code, mssv) ở ô A1 theo mẫu.");
  }
  const emails = [header];
  for (let row = 1; row <= range.e.r; row++) {
    const cell = sheet[XLSX.utils.encode_cell({ r: row, c: 0 })];
    const email = String(cell?.v ?? "").trim();
    if (cell?.f || cell?.t === "e" || /[\r\n]/.test(email)) {
      throw new Error(`Dòng ${row + 1} chứa công thức, ô lỗi hoặc nhiều dòng trong một ô.`);
    }
    emails.push(email);
  }
  return emails.join("\n");
}
