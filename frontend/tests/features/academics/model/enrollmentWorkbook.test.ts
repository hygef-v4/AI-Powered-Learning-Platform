import { describe, expect, it } from "vitest";
import * as XLSX from "xlsx";
import { readEnrollmentWorkbook } from "@/features/academics/model/enrollmentWorkbook";

function createFile(rows: unknown[][]) {
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(rows), "Sinh viên");
  const bytes = XLSX.write(workbook, { type: "array", bookType: "xlsx" }) as ArrayBuffer;
  const file = new File([bytes], "students.xlsx");
  Object.defineProperty(file, "arrayBuffer", { value: async () => bytes });
  return file;
}

describe("Excel enrollment", () => {
  it("reads the email column into the existing preview and enrollment contract", async () => {
    expect(await readEnrollmentWorkbook(createFile([["email"], ["a@example.test"], ["b@example.test"]])))
      .toBe("email\na@example.test\nb@example.test");
  });

  it("rejects missing headers, multiple columns, formulas and embedded newlines", async () => {
    for (const rows of [
      [["name"], ["A"]],
      [["email", "name"], ["a@example.test", "A"]],
      [["email"], [{ f: '"a@example.test"', t: "s", v: "a@example.test" }]],
      [["email"], ["a@example.test\nb@example.test"]],
    ]) {
      await expect(readEnrollmentWorkbook(createFile(rows))).rejects.toThrow();
    }
  });

  it("enforces 200 rows and checks size before reading", async () => {
    const rows = Array.from({ length: 200 }, (_, i) => [`sv${i}@example.test`]);
    await expect(readEnrollmentWorkbook(createFile([["email"], ...rows]))).resolves.toContain("sv199@");
    await expect(readEnrollmentWorkbook(createFile([["email"], ...rows, ["extra@example.test"]])))
      .rejects.toThrow("200");
    await expect(readEnrollmentWorkbook(new File([new Uint8Array(102401)], "big.xlsx")))
      .rejects.toThrow("100 KB");
  });

  it("rejects CSV renamed as xlsx and corrupted workbooks", async () => {
    for (const content of ["email\na@example.test", "PKbroken"]) {
      const file = new File([content], "bad.xlsx");
      Object.defineProperty(file, "arrayBuffer", { value: async () => new TextEncoder().encode(content).buffer });
      await expect(readEnrollmentWorkbook(file)).rejects.toThrow();
    }
  });
});
