"use client";

import { useState, type DragEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Pill, Select, TextInput, cx } from "@/shared/ui";
import { saveGroups } from "./api";
import type { Draft, GroupSetView, Issue, Person } from "./types";

export function personLabel(p: Pick<Person, "name" | "email"> | null | undefined): string {
  return p?.name || p?.email || "Không rõ tên";
}

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

type Props = {
  classId: string;
  set: GroupSetView;
  initial?: Draft[];
  onCancel: () => void;
  onSaved: (next: GroupSetView) => void;
};

const UNGROUPED = "__none";

/**
 * Sửa cả bộ nhóm (t02f, BR-U12-02–04): tạo nhóm, đổi tên, chuyển hoặc gỡ thành viên, chọn trưởng nhóm. Thành viên
 * kéo thả được sang nhóm khác hoặc khu Chưa có nhóm; ô ⇄ Chuyển làm cùng việc bằng bàn phím. Thay đổi chỉ áp dụng
 * khi bấm Lưu cho cả lớp; lỗi hiện theo từng nhóm.
 */
export function GroupEditor({ classId, set, initial, onCancel, onSaved }: Props) {
  const [drafts, setDrafts] = useState<Draft[]>(() => initial ?? draftsFrom(set));
  const [issues, setIssues] = useState<Issue[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [counter, setCounter] = useState(1);
  const [dropKey, setDropKey] = useState<string | null>(null);
  const [dragging, setDragging] = useState<string | null>(null);

  // Người chọn được: sinh viên đang học (thành viên còn hiệu lực và người chưa có nhóm).
  const people = new Map<string, Person>();
  set.groups.forEach((g) => g.members.filter((m) => m.active).forEach((m) => people.set(m.accountId, m)));
  set.ungrouped.forEach((p) => people.set(p.accountId, p));
  const assigned = new Set(drafts.flatMap((d) => d.memberIds));
  const ungrouped = [...people.values()].filter((p) => !assigned.has(p.accountId));

  const update = (key: string, change: (d: Draft) => Draft) => setDrafts((all) => all.map((d) => (d.key === key ? change(d) : d)));

  const move = (accountId: string, target: string) => {
    setDrafts((all) =>
      all.map((d) => {
        const without = d.memberIds.filter((id) => id !== accountId);
        const memberIds = d.key === target ? [...without, accountId] : without;
        return { ...d, memberIds, leaderId: memberIds.includes(d.leaderId ?? "") ? d.leaderId : null };
      }),
    );
  };

  const dragFrom = (accountId: string) => ({
    draggable: true,
    onDragStart: (e: DragEvent) => {
      setDragging(accountId);
      e.dataTransfer?.setData("text/plain", accountId);
      if (e.dataTransfer) e.dataTransfer.effectAllowed = "move";
    },
    onDragEnd: () => {
      setDragging(null);
      setDropKey(null);
    },
  });

  const dropOn = (key: string) => ({
    onDragOver: (e: DragEvent) => {
      if (!dragging) return;
      e.preventDefault();
      setDropKey(key);
    },
    onDragLeave: () => setDropKey((k) => (k === key ? null : k)),
    onDrop: (e: DragEvent) => {
      e.preventDefault();
      const accountId = dragging;
      setDragging(null);
      setDropKey(null);
      if (accountId) move(accountId, key);
    },
  });

  const addGroup = () => {
    const names = new Set(drafts.map((d) => d.name.toLowerCase()));
    let n = drafts.length + counter;
    while (names.has(`nhóm ${n}`)) n++;
    setDrafts((all) => [...all, { key: `new-${counter}`, id: null, name: `Nhóm ${n}`, leaderId: null, memberIds: [] }]);
    setCounter((c) => c + 1);
  };

  const save = async () => {
    setBusy(true);
    setError(null);
    setIssues([]);
    try {
      onSaved(await saveGroups(classId, set.version, drafts));
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: Issue[] }) : undefined;
      if (problem?.code === "GROUP_SET_INVALID" && problem.issues) {
        setIssues(problem.issues);
        setError("Bộ nhóm có lỗi, chưa lưu gì. Sửa các nhóm được đánh dấu rồi lưu lại.");
      } else if (problem?.status === 409) {
        setError(problem.code === "GROUP_CLASS_ARCHIVED" ? "Lớp đã lưu trữ, chỉ xem được nhóm." : "Nhóm vừa được thay đổi ở nơi khác, hãy tải lại.");
      } else {
        setError(problem?.detail ?? "Không lưu được nhóm, thử lại sau.");
      }
    } finally {
      setBusy(false);
    }
  };

  const targets = [{ key: UNGROUPED, name: "Chưa có nhóm" }, ...drafts.map((d) => ({ key: d.key, name: d.name || "(chưa đặt tên)" }))];

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-primary-soft px-3.5 py-2.5 text-[13px] text-primary-ink">
        <span>Đang sửa nhóm. Thay đổi chỉ áp dụng khi bạn bấm Lưu cho cả lớp.</span>
        <span className="flex gap-2">
          <Button size="sm" onClick={onCancel} disabled={busy}>
            Hủy
          </Button>
          <Button size="sm" variant="primary" icon="save" onClick={() => void save()} disabled={busy}>
            Lưu cho cả lớp
          </Button>
        </span>
      </div>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
          {/* Lỗi của nhóm không còn trên màn (vd. đã bấm Xóa nhóm đã dùng): không có thẻ để hiện, nên hiện ở đây. */}
          {issues.some((i) => !drafts.some((d) => d.key === i.groupKey)) && (
            <ul className="mt-1 list-disc pl-4">
              {issues
                .filter((i) => !drafts.some((d) => d.key === i.groupKey))
                .map((i) => (
                  <li key={i.groupKey + i.code}>{i.message}</li>
                ))}
            </ul>
          )}
        </Alert>
      )}
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {drafts.map((d) => {
          const mine = issues.filter((i) => i.groupKey === d.key);
          return (
            <section
              key={d.key}
              aria-label={`Nhóm ${d.name}`}
              {...dropOn(d.key)}
              className={cx(
                "flex flex-col gap-2 rounded-xl border bg-surface p-3.5",
                mine.length ? "border-danger" : dropKey === d.key ? "border-primary bg-primary-soft" : "border-line",
              )}
            >
              <div className="flex items-center gap-2">
                <TextInput
                  aria-label="Tên nhóm"
                  value={d.name}
                  maxLength={100}
                  onChange={(e) => update(d.key, (x) => ({ ...x, name: e.target.value }))}
                  wrapperClassName="flex-1"
                />
                {d.id === null && <Pill tone="primary">(mới)</Pill>}
                <Button
                  size="sm"
                  variant="ghost"
                  icon="close"
                  aria-label={`Xóa ${d.name}`}
                  onClick={() => setDrafts((all) => all.filter((x) => x.key !== d.key))}
                >
                  Xóa
                </Button>
              </div>
              <p className="text-xs text-muted">
                {d.memberIds.length} người · {d.leaderId ? `trưởng nhóm ${personLabel(people.get(d.leaderId))}` : "Chưa có trưởng nhóm"}
              </p>
              <ul className="flex flex-col gap-1.5">
                {d.memberIds.map((id) => (
                  <li key={id} {...dragFrom(id)} className="flex cursor-grab items-center gap-2 text-[13px]">
                    <label className="flex min-w-0 flex-1 items-center gap-2">
                      <input
                        type="radio"
                        name={`leader-${d.key}`}
                        checked={d.leaderId === id}
                        onChange={() => update(d.key, (x) => ({ ...x, leaderId: id }))}
                        aria-label={`Chọn ${personLabel(people.get(id))} làm trưởng nhóm`}
                      />
                      <span className="truncate">{personLabel(people.get(id))}</span>
                      {d.leaderId === id && <Pill tone="warning">Trưởng nhóm</Pill>}
                    </label>
                    <Select
                      aria-label={`Chuyển ${personLabel(people.get(id))}`}
                      value={d.key}
                      onChange={(e) => move(id, e.target.value)}
                      wrapperClassName="h-8 w-36"
                    >
                      {targets.map((t) => (
                        <option key={t.key} value={t.key}>
                          {t.key === d.key ? "⇄ Chuyển" : t.name}
                        </option>
                      ))}
                    </Select>
                  </li>
                ))}
              </ul>
              {mine.map((i) => (
                <p key={i.code + i.message} role="alert" className="text-[11px] text-danger">
                  {i.message}
                </p>
              ))}
            </section>
          );
        })}
        <section
          aria-label="Chưa có nhóm"
          {...dropOn(UNGROUPED)}
          className={cx(
            "flex flex-col gap-2 rounded-xl border border-dashed p-3.5",
            dropKey === UNGROUPED ? "border-primary bg-primary-soft" : "border-line-strong bg-shell",
          )}
        >
          <p className="text-[13px] font-semibold text-ink">Chưa có nhóm ({ungrouped.length})</p>
          <p className="text-[11px] text-muted">Kéo tên sinh viên thả vào nhóm, hoặc dùng ô ⇄ Chuyển.</p>
          <ul className="flex flex-col gap-1.5">
            {ungrouped.map((p) => (
              <li key={p.accountId} {...dragFrom(p.accountId)} className="flex cursor-grab items-center gap-2 text-[13px]">
                <span className="min-w-0 flex-1 truncate">{personLabel(p)}</span>
                <Select
                  aria-label={`Chuyển ${personLabel(p)}`}
                  value={UNGROUPED}
                  onChange={(e) => move(p.accountId, e.target.value)}
                  wrapperClassName="h-8 w-36"
                >
                  {targets.map((t) => (
                    <option key={t.key} value={t.key}>
                      {t.key === UNGROUPED ? "⇄ Chuyển" : t.name}
                    </option>
                  ))}
                </Select>
              </li>
            ))}
          </ul>
          <Button size="sm" icon="add" onClick={addGroup}>
            Thêm nhóm
          </Button>
        </section>
      </div>
    </div>
  );
}
