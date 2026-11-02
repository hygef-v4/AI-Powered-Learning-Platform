import * as XLSX from "xlsx";
import { expect, it } from "vitest";
import { readWorkbook } from "./xlsx";

function workbook() {
  const book = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(book, XLSX.utils.aoa_to_sheet([["Họ tên", "Điểm", "Đạt"], [], ["Nguyễn An", 8.5, true]]), "Điểm");
  XLSX.utils.book_append_sheet(book, XLSX.utils.aoa_to_sheet([]), "Trống");
  return new Blob([XLSX.write(book, { type: "array", bookType: "xlsx" }) as ArrayBuffer]);
}

it("reads every sheet in order with formatted cell text", async () => {
  const sheets = await readWorkbook(workbook());
  expect(sheets.map((s) => s.sheet)).toEqual(["Điểm", "Trống"]);
  expect(sheets[0]!.data[0]).toEqual(["Họ tên", "Điểm", "Đạt"]);
  expect(sheets[0]!.data[2]).toEqual(["Nguyễn An", "8.5", "TRUE"]);
  expect(sheets[1]!.data).toEqual([]);
});
