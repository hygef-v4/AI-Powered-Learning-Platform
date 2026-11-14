import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { expect, it } from "vitest";
import * as XLSX from "xlsx";

it.each([
  ["account-import", ["email", "display_name", "role", "student_code"]],
  ["enrollment-import", ["email"]],
  ["question-import", ["noi_dung", "loai", "lua_chon_1", "lua_chon_2"]],
])("provides a real %s Excel template with headers and examples", (name, headers) => {
  const bytes = readFileSync(resolve("public/templates", `${name}.xlsx`));
  expect(bytes.subarray(0, 2).toString()).toBe("PK");
  const workbook = XLSX.read(bytes, { type: "buffer" });
  const sheet = workbook.Sheets[workbook.SheetNames[0]!]!;
  const rows = XLSX.utils.sheet_to_json<unknown[]>(sheet, { header: 1 });
  expect(rows[0]).toEqual(expect.arrayContaining(headers));
  expect(rows.length).toBeGreaterThan(1);
  expect(Object.values(sheet).some((cell) => typeof cell === "object" && cell !== null && "f" in cell)).toBe(false);
});
