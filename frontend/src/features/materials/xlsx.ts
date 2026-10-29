/** Một sheet Excel đã đọc: tên và bảng giá trị (chuỗi đã định dạng như trong Excel) theo hàng, cột. */
export type Sheet = { sheet: string; data: string[][] };

const MAX_ROWS = 5000;
const MAX_COLUMNS = 200;

/**
 * Đọc tệp .xlsx trên trình duyệt bằng SheetJS: các sheet theo thứ tự trong workbook, mỗi ô là chữ đã định dạng (số, ngày như
 * Excel hiện). Giới hạn 5.000 hàng, 200 cột mỗi sheet. Thư viện chỉ tải khi mở tệp Excel.
 */
export async function readWorkbook(blob: Blob): Promise<Sheet[]> {
  const XLSX = await import("xlsx");
  const workbook = XLSX.read(await blob.arrayBuffer(), { type: "array" });
  if (workbook.SheetNames.length === 0) throw new Error("Không phải tệp Excel");
  return workbook.SheetNames.map((name) => {
    const sheet = workbook.Sheets[name];
    const rows = sheet ? XLSX.utils.sheet_to_json<unknown[]>(sheet, { header: 1, raw: false, defval: "", blankrows: true }) : [];
    const data = rows.slice(0, MAX_ROWS).map((row) => row.slice(0, MAX_COLUMNS).map((cell) => (cell === null || cell === undefined ? "" : String(cell))));
    const width = Math.max(0, ...data.map((row) => row.length));
    return { sheet: name, data: data.map((row) => [...row, ...Array<string>(width - row.length).fill("")]) };
  });
}
