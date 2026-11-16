import { msg } from "@/i18n/msg";

/** Đọc sheet đầu của mẫu Excel (một cột email hoặc MSSV); server vẫn kiểm lại từng dòng và quyền ghi danh. */
const HEADERS = new Set(["email", "student_code", "mssv"]);

export async function readEnrollmentWorkbook(file: File): Promise<string> {
  if (file.size > 100 * 1024) throw new Error(msg("academics.model.enrollmentWorkbook.theListCanBeAt"));
  if (!file.name.toLowerCase().endsWith(".xlsx")) throw new Error(msg("academics.model.enrollmentWorkbook.onlyExcelXlsxFilesAre"));
  const bytes = new Uint8Array(await file.arrayBuffer());
  if (bytes[0] !== 0x50 || bytes[1] !== 0x4b) throw new Error(msg("academics.model.enrollmentWorkbook.notAnExcelXlsxFile"));
  const XLSX = await import("xlsx");
  let workbook;
  try {
    workbook = XLSX.read(bytes, { type: "array", sheetRows: 202 });
  } catch {
    throw new Error(msg("academics.model.enrollmentWorkbook.couldNotReadTheExcel"));
  }
  const sheet = workbook.Sheets[workbook.SheetNames[0] ?? ""];
  if (!sheet?.["!ref"]) throw new Error(msg("academics.model.enrollmentWorkbook.theExcelFileIsEmpty"));
  const range = XLSX.utils.decode_range(sheet["!fullref"] ?? sheet["!ref"]);
  if (range.e.r > 200) throw new Error(msg("academics.model.enrollmentWorkbook.theListCanHaveAt"));
  if (range.e.c > 0) throw new Error(msg("academics.model.enrollmentWorkbook.theExcelFileMayOnly"));
  const header = String(sheet.A1?.v ?? "").trim().toLowerCase();
  if (sheet.A1?.f || !HEADERS.has(header)) {
    throw new Error(msg("academics.model.enrollmentWorkbook.theExcelFileNeedsAn"));
  }
  const emails = [header];
  for (let row = 1; row <= range.e.r; row++) {
    const cell = sheet[XLSX.utils.encode_cell({ r: row, c: 0 })];
    const email = String(cell?.v ?? "").trim();
    if (cell?.f || cell?.t === "e" || /[\r\n]/.test(email)) {
      throw new Error(msg("academics.model.enrollmentWorkbook.rowContainsAFormulaAn", { value: String(row + 1) }));
    }
    emails.push(email);
  }
  return emails.join("\n");
}
