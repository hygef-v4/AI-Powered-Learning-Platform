import { ApiError } from "@/shared/api/client";
import type { Draft, GroupSetView, Issue, Person } from "./types";

/** Một sinh viên đang học trên tab Sinh viên, kèm ngày vào lớp khi trang lớp có danh sách ghi danh. */
export type StudentRow = Person & { enrolledAt?: string | null };

/** Giá trị lọc "Chưa có nhóm". */
export const UNGROUPED = "__none";

export function personLabel(p: Pick<Person, "name" | "email"> | null | undefined): string {
  return p?.name || p?.email || "Không rõ tên";
}

/** Thứ tự tên nhóm tự nhiên: Nhóm 2 trước Nhóm 10. */
export const byGroupName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name, "vi", { numeric: true });

/** Bản sửa từ bộ nhóm hiện có; thành viên đã rời lớp bị bỏ khi lưu (BR-U12-07). */
export function draftsFrom(set: GroupSetView): Draft[] {
  return set.groups.map((g) => ({
    key: g.id,
    id: g.id,
    name: g.name,
    leaderId: g.leaderId,
    memberIds: g.members.filter((m) => m.active).map((m) => m.accountId),
  }));
}

/** `count` tên "Nhóm n" chưa dùng, bắt đầu từ số nhóm hiện có + 1. */
export function freeNames(drafts: Draft[], count: number): string[] {
  const taken = new Set(drafts.map((d) => d.name.trim().toLowerCase()));
  const names: string[] = [];
  for (let n = drafts.length + 1; names.length < count; n++) {
    if (!taken.has(`nhóm ${n}`)) names.push(`Nhóm ${n}`);
  }
  return names;
}

/**
 * Rút `ids` khỏi các nhóm đang có để đưa sang nhóm khác: nhóm mất trưởng nhóm thì người đầu tiên còn lại thay (BR-U12-04);
 * trả cả các nhóm không còn ai.
 */
export function withdraw(drafts: Draft[], ids: string[]): { rest: Draft[]; emptied: Draft[] } {
  const rest: Draft[] = [];
  const emptied: Draft[] = [];
  for (const d of drafts) {
    const memberIds = d.memberIds.filter((id) => !ids.includes(id));
    if (memberIds.length === 0 && d.memberIds.length > 0) {
      emptied.push(d);
      continue;
    }
    const leaderId = d.leaderId && memberIds.includes(d.leaderId) ? d.leaderId : (memberIds[0] ?? null);
    rest.push({ ...d, memberIds, leaderId });
  }
  return { rest, emptied };
}

/** Câu báo lỗi khi lưu bộ nhóm, kèm lỗi của từng nhóm (BR-U12-02–04). */
export function saveErrorText(e: unknown, drafts: Draft[]): string {
  const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: Issue[] }) : undefined;
  if (problem?.code === "GROUP_SET_INVALID" && problem.issues) {
    const lines = problem.issues.map((i) => {
      const d = drafts.find((x) => x.key === i.groupKey);
      return d ? `${d.name}: ${i.message}` : i.message;
    });
    return `Chưa lưu: ${lines.join("; ")}`;
  }
  if (problem?.status === 409) {
    return problem.code === "GROUP_CLASS_ARCHIVED" ? "Lớp đã lưu trữ, chỉ xem được nhóm." : "Nhóm vừa được thay đổi ở nơi khác, hãy tải lại trang.";
  }
  return problem?.detail ?? "Không lưu được nhóm, thử lại sau.";
}
