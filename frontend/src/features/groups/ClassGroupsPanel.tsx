"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Alert, Button, ClickableRow, Dialog, IconButton, PageNav, Pill, TableFrame, Td, TextInput, Th, cx, showToast } from "@/shared/ui";
import { getGroups, saveGroups } from "./api";
import { GroupFormDialog } from "./GroupFormDialog";
import { byGroupName, draftsFrom, personLabel, saveErrorText, UNGROUPED, type StudentRow } from "./groupDrafts";
import { LeaderRequestList } from "./LeaderRequestList";
import { RandomSplitDialog } from "./RandomSplitDialog";
import type { Draft, GroupSetView } from "./types";

const PAGE_SIZE = 20;

type Open = { kind: "create" } | { kind: "edit"; group: Draft } | { kind: "delete"; group: Draft } | { kind: "random" } | null;

const formatDate = (value?: string | null) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });
};

/**
 * Tab Sinh viên của Teacher Class Detail (#32, UC 57), hai thẻ cạnh nhau:
 * - thẻ Nhóm (trái): Tạo nhóm, Chia ngẫu nhiên; danh sách Tất cả, từng nhóm, Chưa có nhóm (có ô tìm khi nhiều nhóm, cuộn trong
 *   thẻ), mỗi mục ghi số sinh viên, nhóm có sẵn nút sửa, xóa; bấm mục để lọc bảng bên cạnh. Tạo, sửa, xóa, chia làm trong popup
 *   và lưu ngay;
 * - bảng sinh viên (phải) chỉ để xem: tiêu đề là mục đang lọc, tìm theo tên hoặc email, bấm dòng để mở Student Detail; cột cuối
 *   có nút Bảng điểm sang trang Chấm điểm của người đó.
 * Báo trang lớp qua `onStudentTable` để trang ẩn bảng sinh viên của mình; không tải được nhóm thì trang giữ bảng đó.
 */
export function ClassGroupsPanel({ classId, students, onView, onGroupNamesLoaded, onStudentTable }: {
  classId: string;
  students?: StudentRow[];
  onView?: (accountId: string) => void;
  onGroupNamesLoaded?: (names: Record<string, string>, leaders?: string[]) => void;
  onStudentTable?: (shown: boolean) => void;
}) {
  const [set, setSet] = useState<GroupSetView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState("");
  const [query, setQuery] = useState("");
  const [groupQuery, setGroupQuery] = useState("");
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<Open>(null);
  const [busy, setBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  // Bảng của U12 thay bảng chỉ xem của trang ngay từ lúc tải; tải lỗi thì trả lại bảng của trang.
  const shown = set !== null || error === null;
  useEffect(() => {
    onStudentTable?.(shown);
  }, [shown, onStudentTable]);
  useEffect(() => () => onStudentTable?.(false), [onStudentTable]);

  const receive = useCallback((loaded: GroupSetView) => {
    setSet(loaded);
    setError(null);
    onGroupNamesLoaded?.(
      Object.fromEntries(loaded.groups.flatMap(group =>
        group.members.filter(member => member.active).map(member => [member.accountId, group.name]),
      )),
      loaded.groups.flatMap(group => group.members.filter(member => member.active && member.leader).map(member => member.accountId)),
    );
  }, [onGroupNamesLoaded]);

  const load = useCallback(
    () => getGroups(classId).then(receive, () => setError("Không tải được nhóm của lớp, thử lại sau.")),
    [classId, receive],
  );

  useEffect(() => {
    let active = true;
    getGroups(classId).then(
      (loaded) => {
        if (active) receive(loaded);
      },
      () => {
        if (active) setError("Không tải được nhóm của lớp, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId, receive]);

  if (error && !set) {
    return (
      <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void load()}>Tải lại</Button>}>
        {error}
      </Alert>
    );
  }
  if (!set) return <p role="status" className="text-[13px] text-muted">Đang tải sinh viên và nhóm…</p>;

  const readOnly = set.classStatus === "ARCHIVED";
  const drafts = draftsFrom(set);
  const sorted = [...drafts].sort(byGroupName);
  // Người trong bảng: sinh viên đang học theo trang lớp, thiếu thì lấy thành viên còn hiệu lực và người chưa có nhóm.
  const people = new Map<string, StudentRow>();
  if (students) students.forEach((s) => people.set(s.accountId, s));
  else {
    set.groups.forEach((g) => g.members.filter((m) => m.active).forEach((m) => people.set(m.accountId, m)));
    set.ungrouped.forEach((p) => people.set(p.accountId, p));
  }
  const all = [...people.values()];
  const groupOf = (accountId: string) => drafts.find((d) => d.memberIds.includes(accountId));
  const ungroupedCount = all.filter((p) => !groupOf(p.accountId)).length;
  const q = query.toLocaleLowerCase("vi").trim();
  const visible = all
    .sort((a, b) => personLabel(a).localeCompare(personLabel(b), "vi"))
    .filter((p) => `${personLabel(p)} ${p.email ?? ""}`.toLocaleLowerCase("vi").includes(q)
      && (!filter || (groupOf(p.accountId)?.key ?? UNGROUPED) === filter));
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const used = (d: Draft) => !!d.id && set.groups.some((g) => g.id === d.id && g.used);

  const saved = (next: GroupSetView, focus?: string) => {
    receive(next);
    setOpen(null);
    const match = focus ? next.groups.find((g) => g.name === focus) : undefined;
    setFilter(match ? match.id : filter && next.groups.some((g) => g.id === filter) ? filter : "");
    setPage(0);
  };

  const remove = async (group: Draft) => {
    const next = drafts.filter((d) => d.key !== group.key);
    setBusy(true);
    setDeleteError(null);
    try {
      saved(await saveGroups(classId, set.version, next));
      showToast(`Đã xóa ${group.name}`, "danger");
    } catch (e) {
      setDeleteError(saveErrorText(e, drafts));
    } finally {
      setBusy(false);
    }
  };

  const gq = groupQuery.toLocaleLowerCase("vi").trim();
  const shownGroups = sorted.filter((d) => d.name.toLocaleLowerCase("vi").includes(gq));
  const pick = (key: string) => { setFilter(key); setPage(0); };
  const selectedName = filter === UNGROUPED ? "Chưa có nhóm" : (drafts.find((d) => d.key === filter)?.name ?? "Tất cả sinh viên");

  return (
    <div className="flex flex-col gap-4">
      <LeaderRequestList set={set} onChanged={() => void load()} />
      <div className="grid min-w-0 items-start gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
        {/* Thẻ Nhóm: mọi việc với nhóm ở đây, mỗi nhóm có sẵn nút sửa, xóa; bấm nhóm để lọc bảng bên cạnh. */}
        <section aria-label="Nhóm" className="flex min-w-0 flex-col gap-3 rounded-xl border border-line bg-surface p-4">
          <h2 className="font-display text-[15px] font-bold text-ink">Nhóm</h2>
          {!readOnly && (
            <div className="grid grid-cols-2 gap-2">
              <Button variant="primary" icon="add" disabled={all.length === 0} onClick={() => setOpen({ kind: "create" })}>Tạo nhóm</Button>
              <Button icon="shuffle" disabled={all.length === 0} onClick={() => setOpen({ kind: "random" })}>Chia ngẫu nhiên</Button>
            </div>
          )}
          {drafts.length > 6 && (
            <TextInput type="search" icon="search" aria-label="Tìm nhóm" placeholder="Tìm nhóm" value={groupQuery} onChange={(e) => setGroupQuery(e.target.value)} />
          )}
          <ul aria-label="Danh sách nhóm" className="-mx-1 flex max-h-[60vh] flex-col gap-0.5 overflow-y-auto px-1 [scrollbar-color:var(--c-muted)_transparent]">
            <GroupItem name="Tất cả" meta={`${all.length} sinh viên`} active={!filter} onPick={() => pick("")} />
            {shownGroups.map((d) => (
              <GroupItem key={d.key} name={d.name} meta={`${d.memberIds.length} sinh viên`}
                active={filter === d.key} onPick={() => pick(d.key)}
                actions={!readOnly && <>
                  <IconButton icon="edit" size={28} label={`Sửa ${d.name}`} onClick={() => setOpen({ kind: "edit", group: d })} />
                  <IconButton icon="trash" size={28} disabled={used(d)} className="text-danger! hover:bg-danger-soft"
                    label={used(d) ? `Không xóa được ${d.name}: nhóm đã có tài liệu hoặc bài nộp` : `Xóa ${d.name}`}
                    onClick={() => { setDeleteError(null); setOpen({ kind: "delete", group: d }); }} />
                </>} />
            ))}
            {drafts.length > 0 && shownGroups.length === 0 && <li className="px-2.5 py-3 text-[13px] text-muted">Không có nhóm khớp.</li>}
            <GroupItem name="Chưa có nhóm" meta={`${ungroupedCount} sinh viên`} active={filter === UNGROUPED} onPick={() => pick(UNGROUPED)} />
          </ul>
        </section>

        {/* Bảng sinh viên chỉ để xem. */}
        <section aria-label="Sinh viên trong lớp" className="flex min-w-0 flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="min-w-0 flex-1 truncate font-display text-[15px] font-bold text-ink" title={selectedName}>{selectedName}</h2>
            <div role="search" className="w-full sm:w-72">
              <TextInput type="search" icon="search" aria-label="Lọc theo tên hoặc email" placeholder="Lọc theo tên hoặc email" value={query}
                onChange={(e) => { setQuery(e.target.value); setPage(0); }} />
            </div>
          </div>
          <TableFrame filledHead className="[&_table]:table-fixed">
            <colgroup><col /><col className="w-[24%]" /><col className="w-[96px]" /><col className="w-[108px]" /><col className="w-[100px]" /></colgroup>
            <thead><tr><Th>Họ tên</Th><Th>Email</Th><Th>Nhóm</Th><Th>Ngày vào lớp</Th><Th><span className="sr-only">Bảng điểm</span></Th></tr></thead>
            <tbody>
              {rows.map((p) => {
                const g = groupOf(p.accountId);
                const cells = <>
                  <Td>
                    <span className="flex min-w-0 items-center gap-2">
                      <span className="truncate font-semibold text-ink">{personLabel(p)}</span>
                      {g?.leaderId === p.accountId && <Pill tone="warning">Trưởng nhóm</Pill>}
                    </span>
                  </Td>
                  <Td className="truncate text-muted">{p.email}</Td>
                  <Td className="truncate">{g?.name ?? <span className="text-muted">Chưa có nhóm</span>}</Td>
                  <Td className="text-muted">{formatDate(p.enrolledAt)}</Td>
                  <Td className="text-right">
                    {/* Sang trang Chấm điểm, chế độ Theo sinh viên với người này. */}
                    <Link href={`/teaching/classes/${encodeURIComponent(classId)}?tab=gradebook&student=${encodeURIComponent(p.accountId)}`}
                      onClick={(event) => event.stopPropagation()}
                      className="inline-flex h-8 items-center rounded-lg border border-line-strong bg-surface px-2.5 text-xs font-semibold whitespace-nowrap text-ink transition-colors hover:border-primary hover:bg-primary-soft hover:text-primary-ink">
                      Bảng điểm
                    </Link>
                  </Td>
                </>;
                return onView
                  ? <ClickableRow key={p.accountId} onClick={() => onView(p.accountId)}>{cells}</ClickableRow>
                  : <tr key={p.accountId}>{cells}</tr>;
              })}
              {rows.length === 0 && <tr><Td colSpan={5} className="py-10 text-center! text-muted">Không có sinh viên phù hợp.</Td></tr>}
            </tbody>
          </TableFrame>
          <PageNav page={current} size={PAGE_SIZE} total={visible.length} onPage={setPage} />
        </section>
      </div>

      {(open?.kind === "create" || open?.kind === "edit") && (
        <GroupFormDialog classId={classId} set={set} people={all} group={open.kind === "edit" ? open.group : undefined}
          onClose={() => setOpen(null)} onSaved={(next, name) => saved(next, name)} />
      )}
      {open?.kind === "random" && (
        <RandomSplitDialog classId={classId} set={set} people={all} onClose={() => setOpen(null)} onSaved={(next) => saved(next)} />
      )}
      {open?.kind === "delete" && (
        <Dialog open title={`Xóa ${open.group.name}?`} onClose={() => setOpen(null)} dismissible={!busy}
          footer={<Button variant="danger" disabled={busy} onClick={() => void remove(open.group)}>Xóa nhóm</Button>}>
          <p>Bạn có chắc muốn xóa {open.group.name} không?</p>
          {deleteError && <Alert tone="danger" role="alert">{deleteError}</Alert>}
        </Dialog>
      )}
    </div>
  );
}

/** Một dòng trong thẻ Nhóm: tên, dòng phụ, nút sửa và xóa luôn hiện bên phải; bấm dòng để lọc bảng sinh viên. */
function GroupItem({ name, meta, active, onPick, actions }: { name: string; meta: string; active: boolean; onPick: () => void; actions?: ReactNode }) {
  return (
    <li className={cx("row-interactive flex items-center gap-1 rounded-lg pr-1", active ? "bg-primary-soft" : "hover:bg-shell")}>
      <button type="button" aria-pressed={active} onClick={onPick} className="min-w-0 flex-1 px-2.5 py-2 text-left">
        <span className={cx("block truncate text-[13.5px] font-semibold", active ? "text-primary-ink" : "text-ink")} title={name}>{name}</span>
        <span className="block truncate text-xs text-muted">{meta}</span>
      </button>
      {actions}
    </li>
  );
}
