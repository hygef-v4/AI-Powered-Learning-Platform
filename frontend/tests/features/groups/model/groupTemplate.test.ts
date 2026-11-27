import { describe, expect, it, vi } from "vitest";
import * as XLSX from "xlsx";
import { downloadGroupTemplate, readGroupWorkbook } from "@/features/groups/model/groupWorkbook";

const written: { workbook: XLSX.WorkBook; name: string }[] = [];

vi.mock("xlsx", async (importOriginal) => {
  const actual = await importOriginal<typeof import("xlsx")>();
  return { ...actual, writeFile: (workbook: XLSX.WorkBook, name: string) => { written.push({ workbook, name }); } };
});

describe("downloadGroupTemplate", () => {
  it("pre-fills ungrouped students and reads back as a valid import file", async () => {
    await downloadGroupTemplate([
      { accountId: "a", name: "An", email: "an@fpt.edu.vn", studentCode: "SE180001" },
      { accountId: "c", name: "Chi", email: "chi@fpt.edu.vn", studentCode: null },
    ], "mau-nhom.xlsx");

    const { workbook, name } = written[0]!;
    expect(name).toBe("mau-nhom.xlsx");
    expect(workbook.SheetNames).toEqual(["Nhóm", "Hướng dẫn"]);
    expect(XLSX.utils.sheet_to_json(workbook.Sheets["Nhóm"]!, { header: 1 })).toEqual([
      ["MSSV", "Email", "Họ tên", "Tên nhóm", "Trưởng nhóm"],
      ["SE180001", "an@fpt.edu.vn", "An", "", ""],
      ["", "chi@fpt.edu.vn", "Chi", "", ""],
    ]);

    const bytes = XLSX.write(workbook, { type: "array", bookType: "xlsx" }) as ArrayBuffer;
    const file = new File([bytes], name);
    Object.defineProperty(file, "arrayBuffer", { value: async () => bytes });
    expect(await readGroupWorkbook(file)).toEqual([
      { row: 2, studentCode: "SE180001", email: "an@fpt.edu.vn", groupName: "", leader: false },
      { row: 3, studentCode: "", email: "chi@fpt.edu.vn", groupName: "", leader: false },
    ]);
  });
});
