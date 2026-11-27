import { describe, expect, it } from "vitest";
import * as XLSX from "xlsx";
import { buildGroupImport, readGroupWorkbook, type GroupImportRow } from "@/features/groups/model/groupWorkbook";
import type { GroupSetView, Person } from "@/features/groups/model/types";

function createFile(rows: unknown[][], name = "groups.xlsx") {
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(rows), "Nhóm");
  const bytes = XLSX.write(workbook, { type: "array", bookType: "xlsx" }) as ArrayBuffer;
  const file = new File([bytes], name);
  Object.defineProperty(file, "arrayBuffer", { value: async () => bytes });
  return file;
}

const people: Person[] = [
  { accountId: "a", name: "An", email: "an@fpt.edu.vn", studentCode: "SE180001" },
  { accountId: "b", name: "Bình", email: "binh@fpt.edu.vn", studentCode: "SE180002" },
  { accountId: "c", name: "Chi", email: "chi@fpt.edu.vn", studentCode: null },
  { accountId: "d", name: "Dũng", email: "dung@fpt.edu.vn", studentCode: "SE180004" },
  { accountId: "e", name: "Em", email: "em@fpt.edu.vn", studentCode: "SE180005" },
];

const set: GroupSetView = {
  classId: "c1",
  classStatus: "OPEN",
  version: 3,
  groups: [{
    id: "g1",
    name: "Nhóm 1",
    leaderId: "e",
    members: [{ accountId: "e", name: "Em", email: "em@fpt.edu.vn", active: true, leader: true }],
    used: false,
  }],
  ungrouped: people.slice(0, 4),
  pendingRequests: [],
};

const row = (n: number, studentCode: string, groupName: string, leader = false, email = ""): GroupImportRow =>
  ({ row: n, studentCode, email, groupName, leader });

describe("readGroupWorkbook", () => {
  it("reads the template columns in any order, with Vietnamese or English headers", async () => {
    const rows = await readGroupWorkbook(createFile([
      ["Tên nhóm", "Họ tên", "MSSV", "Email", "Trưởng nhóm"],
      ["  Nhóm  A ", "An", "SE180001", "", "x"],
      ["", "Bình", "SE180002", "", ""],
      ["", "", "", "", ""],
    ]));
    expect(rows).toEqual([
      { row: 2, studentCode: "SE180001", email: "", groupName: "Nhóm A", leader: true },
      { row: 3, studentCode: "SE180002", email: "", groupName: "", leader: false },
    ]);
    const english = await readGroupWorkbook(createFile([["Email", "Group name", "Leader"], ["chi@fpt.edu.vn", "Team", ""]]));
    expect(english).toEqual([{ row: 2, studentCode: "", email: "chi@fpt.edu.vn", groupName: "Team", leader: false }]);
  });

  it("rejects files without the needed headers, with formulas, too many rows or not xlsx", async () => {
    await expect(readGroupWorkbook(createFile([["MSSV", "Họ tên"], ["SE1", "A"]]))).rejects.toThrow();
    await expect(readGroupWorkbook(createFile([["MSSV", "Tên nhóm"], [{ t: "s", v: "SE1", f: '"SE1"' }, "N"]])))
      .rejects.toThrow("2");
    const many = Array.from({ length: 501 }, (_, i) => [`SE${i}`, "N"]);
    await expect(readGroupWorkbook(createFile([["MSSV", "Tên nhóm"], ...many]))).rejects.toThrow("500");
    await expect(readGroupWorkbook(new File(["MSSV,Tên nhóm"], "groups.csv"))).rejects.toThrow();
    await expect(readGroupWorkbook(new File([new Uint8Array(200 * 1024 + 1)], "big.xlsx"))).rejects.toThrow("200 KB");
  });
});

describe("buildGroupImport", () => {
  it("groups rows by name case-insensitively and takes the single marked leader", () => {
    const preview = buildGroupImport([
      row(2, "SE180001", "Nhóm A", true),
      row(3, "se180002", "nhóm a"),
      row(4, "", "Nhóm B", false, "CHI@fpt.edu.vn"),
      row(5, "SE180004", ""),
    ], { set, people });
    expect(preview.issues).toEqual([]);
    expect(preview.groups.map((g) => [g.name, g.memberIds, g.leaderId, g.leaderCount])).toEqual([
      ["Nhóm A", ["a", "b"], "a", 1],
      ["Nhóm B", ["c"], null, 0],
    ]);
  });

  it("skips rows outside the class, already grouped, duplicated, too long or using an existing group name", () => {
    const preview = buildGroupImport([
      row(2, "SE999999", "Nhóm A"),
      row(3, "SE180005", "Nhóm A"),
      row(4, "SE180001", "Nhóm A", true),
      row(5, "SE180001", "Nhóm B"),
      row(6, "SE180002", "x".repeat(101)),
      row(7, "SE180004", "NHÓM 1"),
      row(8, "", "Nhóm C"),
    ], { set, people });
    expect(preview.issues.map((i) => [i.row, i.code])).toEqual([
      [2, "NOT_IN_CLASS"],
      [3, "ALREADY_GROUPED"],
      [5, "DUPLICATE_STUDENT"],
      [6, "NAME_TOO_LONG"],
      [7, "GROUP_NAME_TAKEN"],
      [8, "MISSING_STUDENT"],
    ]);
    expect(preview.groups.map((g) => g.memberIds)).toEqual([["a"]]);
  });

  it("flags several leaders, sizes outside the self-grouping range and students in proposals", () => {
    const preview = buildGroupImport([
      row(2, "SE180001", "Nhóm A", true),
      row(3, "SE180002", "Nhóm A", true),
      row(4, "SE180004", "Nhóm A"),
      row(5, "", "Nhóm B", true, "chi@fpt.edu.vn"),
    ], { set, people, sizes: { minSize: 2, maxSize: 2 }, proposalOf: new Map([["b", "Đề xuất X"]]) });
    const [a, b] = preview.groups;
    expect(a).toMatchObject({ leaderId: null, leaderCount: 2, sizeWarning: "ABOVE_MAX", inProposal: [{ accountId: "b", proposalName: "Đề xuất X" }] });
    expect(b).toMatchObject({ leaderId: "c", sizeWarning: "BELOW_MIN", inProposal: [] });
  });
});
