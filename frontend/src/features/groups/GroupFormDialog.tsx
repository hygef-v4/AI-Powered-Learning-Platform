"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, Pill, Select, TextInput, cx, showToast } from "@/shared/ui";
import { saveGroups } from "./api";
import { draftsFrom, freeNames, personLabel, saveErrorText, withdraw, type StudentRow } from "./groupDrafts";
import type { Draft, GroupSetView } from "./types";

/**
 * Popup Tạo nhóm / Sửa nhóm (t02f, BR-U12-02–04): tên nhóm, chọn thành viên trong danh sách người chưa có nhóm có ô tìm
 * (sửa nhóm thì thành viên hiện tại đứng đầu), chọn trưởng nhóm trong số thành viên. Bấm lưu là lưu ngay cho cả
 * lớp; nhóm khác không còn ai thì bị bỏ.
 */
export function GroupFormDialog({ classId, set, people, group, onClose, onSaved }: {
  classId: string;
  set: GroupSetView;
  people: StudentRow[];
  /** Nhóm đang sửa; không có là tạo nhóm mới. */
  group?: Draft;
  onClose: () => void;
  onSaved: (next: GroupSetView, name: string) => void;
}) {
  const id = useId();
  const drafts = draftsFrom(set);
  const others = drafts.filter((d) => d.key !== group?.key);
  const [name, setName] = useState(group?.name ?? freeNames(drafts, 1)[0]!);
  const [members, setMembers] = useState<string[]>(group?.memberIds ?? []);
  const [leaderId, setLeaderId] = useState<string | null>(group?.leaderId ?? null);
  const [query, setQuery] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const groupOf = (accountId: string) => others.find((d) => d.memberIds.includes(accountId));
  const q = query.toLocaleLowerCase("vi").trim();
  // Chỉ người chưa có nhóm (sửa nhóm thì thêm thành viên hiện tại, đứng đầu); trong mỗi phần theo tên.
  const rank = (accountId: string) => (group?.memberIds.includes(accountId) ? 0 : 1);
  const list = [...people]
    .filter((p) => !groupOf(p.accountId) && `${personLabel(p)} ${p.email ?? ""}`.toLocaleLowerCase("vi").includes(q))
    .sort((a, b) => rank(a.accountId) - rank(b.accountId) || personLabel(a).localeCompare(personLabel(b), "vi"));
  const leader = leaderId && members.includes(leaderId) ? leaderId : (members[0] ?? null);
  const { rest, emptied } = withdraw(others, members);
  const usedIds = new Set(set.groups.filter((g) => g.used).map((g) => g.id));
  const blocked = emptied.filter((d) => d.id && usedIds.has(d.id));
  const moved = members.filter((m) => groupOf(m)).length;
  const trimmed = name.trim();
  const duplicate = others.some((d) => d.name.trim().toLowerCase() === trimmed.toLowerCase());
  const nameError = !trimmed ? "Nhập tên nhóm" : duplicate ? "Tên nhóm bị trùng" : null;
  const valid = !nameError && members.length > 0 && blocked.length === 0;

  const toggle = (accountId: string) =>
    setMembers((all) => (all.includes(accountId) ? all.filter((x) => x !== accountId) : [...all, accountId]));

  const save = async () => {
    const mine: Draft = { key: group?.key ?? "new-1", id: group?.id ?? null, name: trimmed, leaderId: leader, memberIds: members };
    // Sửa thì giữ nhóm ở chỗ cũ trong bộ nhóm, tạo thì thêm cuối.
    const next = group ? drafts.flatMap((d) => (d.key === group.key ? [mine] : rest.filter((r) => r.key === d.key))) : [...rest, mine];
    setBusy(true);
    setError(null);
    try {
      onSaved(await saveGroups(classId, set.version, next), trimmed);
      showToast(group ? "Đã lưu nhóm" : `Đã tạo ${trimmed}`);
    } catch (e) {
      setError(saveErrorText(e, next));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open title={group ? `Sửa ${group.name}` : "Tạo nhóm"} size="lg" onClose={onClose} dismissible={!busy}
      footer={<Button variant="primary" icon="save" disabled={!valid || busy} onClick={() => void save()}>{group ? "Lưu nhóm" : "Tạo nhóm"}</Button>}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Tên nhóm" htmlFor={`${id}-name`} required error={nameError}>
          <TextInput id={`${id}-name`} value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Trưởng nhóm" htmlFor={`${id}-leader`} required>
          <Select id={`${id}-leader`} value={leader ?? ""} disabled={members.length === 0} onChange={(e) => setLeaderId(e.target.value)}>
            {members.length === 0 && <option value="">Chọn thành viên trước</option>}
            {members.map((m) => <option key={m} value={m}>{personLabel(people.find((p) => p.accountId === m))}</option>)}
          </Select>
        </Field>
      </div>
      <div className="flex flex-col gap-2">
        <span className="text-xs font-semibold text-body">Thành viên ({members.length})</span>
        <TextInput type="search" icon="search" aria-label="Tìm sinh viên" placeholder="Tìm sinh viên" value={query} onChange={(e) => setQuery(e.target.value)} />
        <ul aria-label="Chọn thành viên" className="max-h-72 overflow-y-auto rounded-lg border border-line [scrollbar-color:var(--c-muted)_transparent]">
          {list.map((p) => {
            const picked = members.includes(p.accountId);
            const current = groupOf(p.accountId);
            return (
              <li key={p.accountId} className="border-b border-line last:border-b-0">
                <label className={cx("flex cursor-pointer items-center gap-3 px-3 py-2 transition-colors", picked ? "bg-primary-soft/50" : "hover:bg-shell")}>
                  <input type="checkbox" className="size-4 accent-primary" checked={picked} onChange={() => toggle(p.accountId)} aria-label={`Chọn ${personLabel(p)}`} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13.5px] font-semibold text-ink">{personLabel(p)}</span>
                    <span className="block truncate text-xs text-muted">{p.email}</span>
                  </span>
                  {picked && p.accountId === leader && <Pill tone="warning">Trưởng nhóm</Pill>}
                  {current && <span className="shrink-0 text-xs text-muted">{current.name}</span>}
                </label>
              </li>
            );
          })}
          {list.length === 0 && (
            <li className="px-3 py-8 text-center text-[13px] text-muted">{q ? "Không có sinh viên phù hợp." : "Mọi sinh viên đã có nhóm."}</li>
          )}
        </ul>
      </div>
      {(moved > 0 || emptied.length > 0) && (
        <p className={cx("text-[13px]", blocked.length ? "text-danger" : "text-muted")}>
          {moved > 0 && `Chuyển ${moved} người từ nhóm khác sang. `}
          {blocked.length > 0
            ? `${blocked.map((d) => d.name).join(", ")} đã có bài nên không được để trống.`
            : emptied.length > 0 && `${emptied.map((d) => d.name).join(", ")} không còn ai sẽ bị xóa.`}
        </p>
      )}
    </Dialog>
  );
}
