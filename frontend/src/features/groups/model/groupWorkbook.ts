import { msg } from "@/i18n/msg";
import { buildDraftsFrom } from "./groupDrafts";
import type { GroupSetView, Person } from "./types";

/**
 * Nhập nhóm từ Excel (BR-U12-99…103): mẫu sinh theo lớp, đọc file ở trình duyệt, ghép các dòng cùng tên nhóm thành nhóm mới.
 * Server vẫn kiểm lại cả bộ nhóm khi lưu (BR-U12-02, 04).
 */

export const MAX_FILE_BYTES = 200 * 1024;
export const MAX_ROWS = 500;
export const MAX_GROUP_NAME = 100;

/** Một dòng có dữ liệu của sheet đầu; `row` là số dòng trong Excel (dòng tiêu đề là 1). */
export type GroupImportRow = { row: number; studentCode: string; email: string; groupName: string; leader: boolean };

export type RowIssueCode =
  | "MISSING_STUDENT"
  | "NOT_IN_CLASS"
  | "ALREADY_GROUPED"
  | "DUPLICATE_STUDENT"
  | "NAME_TOO_LONG"
  | "GROUP_NAME_TAKEN";

export type RowIssue = { row: number; code: RowIssueCode; student: string; groupName: string };

export type SizeWarning = "BELOW_MIN" | "ABOVE_MAX";

/** Nhóm mới từ file; `leaderId` rỗng khi file không đánh dấu hoặc đánh dấu nhiều trưởng nhóm (`leaderCount`). */
export type ImportGroup = {
  key: string;
  name: string;
  memberIds: string[];
  leaderId: string | null;
  leaderCount: number;
  sizeWarning: SizeWarning | null;
  /** Thành viên đang ở đề xuất nhóm; lưu thì họ rời đề xuất (BR-U12-88). */
  inProposal: { accountId: string; proposalName: string }[];
};

export type GroupImportPreview = { groups: ImportGroup[]; issues: RowIssue[] };

type Column = "studentCode" | "email" | "name" | "groupName" | "leader";

const HEADER_ALIASES: Record<string, Column> = {
  mssv: "studentCode",
  masv: "studentCode",
  masinhvien: "studentCode",
  studentcode: "studentCode",
  studentid: "studentCode",
  email: "email",
  hoten: "name",
  fullname: "name",
  name: "name",
  tennhom: "groupName",
  nhom: "groupName",
  group: "groupName",
  groupname: "groupName",
  truongnhom: "leader",
  leader: "leader",
  groupleader: "leader",
};

/** Tiêu đề so sánh không dấu, không khoảng trắng, không phân biệt hoa thường. */
function normalizeHeader(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/gi, "d")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}

const normalizeCode = (value: string) => value.trim().toUpperCase();
const normalizeEmail = (value: string) => value.trim().toLowerCase();
const normalizeGroupName = (value: string) => value.trim().replace(/\s+/g, " ");

/** Giá trị phủ định rõ ràng ở cột Trưởng nhóm ("Không", "0", FALSE…) không tính là trưởng nhóm (BR-U12-100). */
const NOT_LEADER = new Set(["khong", "k", "no", "n", "0", "false", "-"]);

function isLeaderMark(value: string): boolean {
  return value !== "" && !NOT_LEADER.has(normalizeHeader(value) || value.trim());
}

/** Tải mẫu điền sẵn sinh viên chưa có nhóm (MSSV, email, họ tên); sheet thứ hai là hướng dẫn. */
export async function downloadGroupTemplate(people: Person[], fileName: string): Promise<void> {
  const XLSX = await import("xlsx");
  const header = [
    msg("groups.model.groupWorkbook.headerStudentCode"),
    "Email",
    msg("groups.model.groupWorkbook.headerFullName"),
    msg("groups.model.groupWorkbook.headerGroupName"),
    msg("groups.model.groupWorkbook.headerLeader"),
  ];
  const rows = people.map((p) => [p.studentCode ?? "", p.email ?? "", p.name ?? "", "", ""]);
  const sheet = XLSX.utils.aoa_to_sheet([header, ...rows]);
  sheet["!cols"] = [{ wch: 14 }, { wch: 30 }, { wch: 28 }, { wch: 24 }, { wch: 14 }];
  const guide = XLSX.utils.aoa_to_sheet([
    [msg("groups.model.groupWorkbook.guideTitle")],
    [msg("groups.model.groupWorkbook.guideRows")],
    [msg("groups.model.groupWorkbook.guideLeader")],
    [msg("groups.model.groupWorkbook.guideBlank")],
    [msg("groups.model.groupWorkbook.guideIdentity")],
    [msg("groups.model.groupWorkbook.guideExisting")],
  ]);
  guide["!cols"] = [{ wch: 110 }];
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, sheet, msg("groups.model.groupWorkbook.sheetGroups"));
  XLSX.utils.book_append_sheet(workbook, guide, msg("groups.model.groupWorkbook.sheetGuide"));
  XLSX.writeFile(workbook, fileName);
}

/** Đọc sheet đầu: kiểm loại, cỡ, số dòng, tiêu đề, công thức; trả các dòng có dữ liệu (BR-U12-101). */
export async function readGroupWorkbook(file: File): Promise<GroupImportRow[]> {
  if (file.size > MAX_FILE_BYTES) throw new Error(msg("groups.model.groupWorkbook.fileTooLarge"));
  if (!file.name.toLowerCase().endsWith(".xlsx")) throw new Error(msg("groups.model.groupWorkbook.onlyXlsx"));
  const bytes = new Uint8Array(await file.arrayBuffer());
  if (bytes[0] !== 0x50 || bytes[1] !== 0x4b) throw new Error(msg("groups.model.groupWorkbook.notXlsx"));
  const XLSX = await import("xlsx");
  let workbook;
  try {
    workbook = XLSX.read(bytes, { type: "array", sheetRows: MAX_ROWS + 2 });
  } catch {
    throw new Error(msg("groups.model.groupWorkbook.couldNotRead"));
  }
  const sheet = workbook.Sheets[workbook.SheetNames[0] ?? ""];
  if (!sheet?.["!ref"]) throw new Error(msg("groups.model.groupWorkbook.emptyFile"));
  const range = XLSX.utils.decode_range(sheet["!fullref"] ?? sheet["!ref"]);
  if (range.e.r > MAX_ROWS) {
    throw new Error(msg("groups.model.groupWorkbook.tooManyRows", { max: String(MAX_ROWS) }));
  }

  const columns = new Map<Column, number>();
  for (let c = range.s.c; c <= range.e.c; c++) {
    const cell = sheet[XLSX.utils.encode_cell({ r: 0, c })];
    const column = HEADER_ALIASES[normalizeHeader(String(cell?.v ?? ""))];
    if (column && !columns.has(column)) columns.set(column, c);
  }
  if (!columns.has("groupName") || (!columns.has("studentCode") && !columns.has("email"))) {
    throw new Error(msg("groups.model.groupWorkbook.missingHeaders"));
  }

  const readCell = (r: number, column: Column): string => {
    const c = columns.get(column);
    if (c === undefined) return "";
    const cell = sheet[XLSX.utils.encode_cell({ r, c })];
    if (!cell) return "";
    if (cell.f || cell.t === "e") {
      throw new Error(msg("groups.model.groupWorkbook.formulaNotAllowed", { row: String(r + 1) }));
    }
    const text = String(cell.w ?? cell.v ?? "").trim();
    if (/[\r\n]/.test(text)) {
      throw new Error(msg("groups.model.groupWorkbook.formulaNotAllowed", { row: String(r + 1) }));
    }
    return text;
  };

  const rows: GroupImportRow[] = [];
  for (let r = 1; r <= range.e.r; r++) {
    const row = {
      row: r + 1,
      studentCode: readCell(r, "studentCode"),
      email: readCell(r, "email"),
      groupName: normalizeGroupName(readCell(r, "groupName")),
      leader: isLeaderMark(readCell(r, "leader")),
    };
    if (row.studentCode || row.email || row.groupName) rows.push(row);
  }
  return rows;
}

/**
 * Ghép dòng thành nhóm mới (BR-U12-102). Chỉ thêm nhóm: dòng lỗi bị bỏ qua và báo theo số dòng; dòng để trống Tên nhóm là
 * sinh viên chưa xếp nhóm. `people` là sinh viên đang học của lớp; `proposalOf` cho biết sinh viên đang ở đề xuất nào.
 */
export function buildGroupImport(
  rows: GroupImportRow[],
  options: {
    set: GroupSetView;
    people: Person[];
    sizes?: { minSize: number; maxSize: number } | null;
    proposalOf?: Map<string, string>;
  },
): GroupImportPreview {
  const { set, people, sizes, proposalOf } = options;
  const byCode = new Map<string, Person>();
  const byEmail = new Map<string, Person>();
  for (const p of people) {
    if (p.studentCode) byCode.set(normalizeCode(p.studentCode), p);
    if (p.email) byEmail.set(normalizeEmail(p.email), p);
  }
  const grouped = new Set(buildDraftsFrom(set).flatMap((d) => d.memberIds));
  const takenNames = new Set(set.groups.map((g) => g.name.trim().toLocaleLowerCase("vi")));
  const seen = new Set<string>();
  const issues: RowIssue[] = [];
  const groups = new Map<string, { name: string; members: string[]; leaders: string[] }>();

  for (const row of rows) {
    if (!row.groupName) continue;
    const student = row.studentCode || row.email;
    const fail = (code: RowIssueCode) => issues.push({ row: row.row, code, student, groupName: row.groupName });
    if (!student) {
      fail("MISSING_STUDENT");
      continue;
    }
    if (row.groupName.length > MAX_GROUP_NAME) {
      fail("NAME_TOO_LONG");
      continue;
    }
    const nameKey = row.groupName.toLocaleLowerCase("vi");
    if (takenNames.has(nameKey)) {
      fail("GROUP_NAME_TAKEN");
      continue;
    }
    // BR-U12-101: nhận theo MSSV; chỉ dòng không có MSSV mới nhận theo email (MSSV sai không tự lấy người theo email).
    const person = (row.studentCode
      ? byCode.get(normalizeCode(row.studentCode))
      : byEmail.get(normalizeEmail(row.email))) ?? null;
    if (!person) {
      fail("NOT_IN_CLASS");
      continue;
    }
    if (grouped.has(person.accountId)) {
      fail("ALREADY_GROUPED");
      continue;
    }
    if (seen.has(person.accountId)) {
      fail("DUPLICATE_STUDENT");
      continue;
    }
    seen.add(person.accountId);
    const group = groups.get(nameKey) ?? { name: row.groupName, members: [], leaders: [] };
    group.members.push(person.accountId);
    if (row.leader) group.leaders.push(person.accountId);
    groups.set(nameKey, group);
  }

  const result: ImportGroup[] = [...groups.values()].map((g, index) => {
    const size = g.members.length;
    const sizeWarning: SizeWarning | null = !sizes
      ? null
      : size < sizes.minSize ? "BELOW_MIN" : size > sizes.maxSize ? "ABOVE_MAX" : null;
    return {
      key: `import-${index + 1}`,
      name: g.name,
      memberIds: g.members,
      leaderId: g.leaders.length === 1 ? g.leaders[0]! : null,
      leaderCount: g.leaders.length,
      sizeWarning,
      inProposal: g.members.flatMap((accountId) => {
        const proposalName = proposalOf?.get(accountId);
        return proposalName ? [{ accountId, proposalName }] : [];
      }),
    };
  });
  return { groups: result, issues };
}

export function describeRowIssue(issue: RowIssue): string {
  const params = { row: String(issue.row), student: issue.student, name: issue.groupName };
  switch (issue.code) {
    case "MISSING_STUDENT":
      return msg("groups.model.groupWorkbook.issueMissingStudent", params);
    case "NOT_IN_CLASS":
      return msg("groups.model.groupWorkbook.issueNotInClass", params);
    case "ALREADY_GROUPED":
      return msg("groups.model.groupWorkbook.issueAlreadyGrouped", params);
    case "DUPLICATE_STUDENT":
      return msg("groups.model.groupWorkbook.issueDuplicateStudent", params);
    case "NAME_TOO_LONG":
      return msg("groups.model.groupWorkbook.issueNameTooLong", params);
    case "GROUP_NAME_TAKEN":
      return msg("groups.model.groupWorkbook.issueGroupNameTaken", params);
  }
}
