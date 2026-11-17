import { ApiError } from "@/shared/api/client";
import type { Draft, GroupSetView, Issue, Person } from "./types";
import { msg } from "@/i18n/msg";

/** Một sinh viên đang học trên tab Sinh viên, kèm ngày vào lớp khi trang lớp có danh sách ghi danh. */
export type StudentRow = Person & { enrolledAt?: string | null };

/** Giá trị lọc "Chưa có nhóm". */
export const UNGROUPED = "__none";

export function formatPersonLabel(p: Pick<Person, "name" | "email"> | null | undefined): string {
  return p?.name || p?.email || msg("groups.model.groupDrafts.unknownName");
}

/** Thứ tự tên nhóm tự nhiên: Nhóm 2 trước Nhóm 10. */
export const compareGroupName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name, "vi", { numeric: true });

/** Bản sửa từ bộ nhóm hiện có; thành viên đã rời lớp bị bỏ khi lưu (BR-U12-07). */
export function buildDraftsFrom(set: GroupSetView): Draft[] {
  return set.groups.map((g) => ({
    key: g.id,
    id: g.id,
    name: g.name,
    leaderId: g.leaderId,
    memberIds: g.members.filter((m) => m.active).map((m) => m.accountId),
  }));
}

/** `count` tên "Nhóm n" chưa dùng, bắt đầu từ số nhóm hiện có + 1. */
export function pickFreeNames(drafts: Draft[], count: number): string[] {
  const taken = new Set(drafts.map((d) => d.name.trim().toLowerCase()));
  const names: string[] = [];
  for (let n = drafts.length + 1; names.length < count; n++) {
    if (!taken.has(msg("groups.model.groupDrafts.group", { n: String(n) }))) names.push(msg("groups.model.groupDrafts.group2", { n: String(n) }));
  }
  return names;
}

/**
 * Rút `ids` khỏi các nhóm đang có để đưa sang nhóm khác: nhóm mất trưởng nhóm thì người đầu tiên còn lại thay (BR-U12-04);
 * trả cả các nhóm không còn ai.
 */
export function withdrawMembers(drafts: Draft[], ids: string[]): { rest: Draft[]; emptied: Draft[] } {
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
    return msg("groups.model.groupDrafts.notSaved", { value: String(lines.join("; ")) });
  }
  if (problem?.status === 409) {
    return problem.code === "GROUP_CLASS_ARCHIVED" ? msg("groups.model.groupDrafts.theClassIsArchivedGroups") : msg("groups.model.groupDrafts.theGroupsWereJustChanged");
  }
  return problem?.detail ?? msg("groups.model.groupDrafts.couldNotSaveTheGroups");
}
