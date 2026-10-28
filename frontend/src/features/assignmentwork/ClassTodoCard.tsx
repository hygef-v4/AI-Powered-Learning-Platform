"use client";

import Link from "next/link";
import { useState } from "react";
import { Pill, Section, cx } from "@/shared/ui";
import { DeadlineChip } from "./DeadlineChip";
import { formatDateTime } from "./format";
import type { StudentRow } from "./types";
import { rowStatus, upcoming, useStudentAssignments } from "./useStudentAssignments";

type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TABS: { mode: Kind; label: string; empty: string }[] = [
  { mode: "INDIVIDUAL", label: "Bài cá nhân", empty: "Không còn bài cá nhân cần làm." },
  { mode: "GROUP", label: "Bài nhóm", empty: "Không còn bài nhóm cần làm." },
  { mode: "PRACTICE", label: "Luyện tập", empty: "Không còn bài luyện tập cần làm." },
];

/** Loại bài như trên bảng Bài tập: bài luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function kindOf(row: StudentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/**
 * "Bài cần làm" trên trang lớp của sinh viên: bài đang mở, chưa nộp và còn hạn, chia ba tab Bài cá nhân / Bài nhóm / Luyện tập
 * (kèm số bài), hạn gần nhất đứng đầu. Danh sách cao đúng 5 dòng, nhiều hơn thì cuộn trong thẻ; gắn qua StudentClassBoard của U05.
 */
export function ClassTodoCard({ classId }: { classId: string }) {
  const { rows, error } = useStudentAssignments(classId);
  const [mode, setMode] = useState<Kind>("INDIVIDUAL");
  // Mốc "còn hạn" lấy một lần lúc mở trang.
  const [now] = useState(() => Date.now());
  const todo = (rows ?? [])
    .filter((row) => upcoming(row) && (!row.closesAt || new Date(row.closesAt).getTime() > now))
    .sort((a, b) => (a.closesAt ? new Date(a.closesAt).getTime() : Infinity) - (b.closesAt ? new Date(b.closesAt).getTime() : Infinity));
  const shown = todo.filter((row) => kindOf(row) === mode);
  return (
    <Section
      title="Bài cần làm"
      actions={<Link href={`/my-assignments?classId=${encodeURIComponent(classId)}`} className="text-[13px] font-semibold text-primary-ink hover:underline">Tất cả bài</Link>}
    >
      <div role="tablist" aria-label="Loại bài" className="-mt-1 flex w-fit gap-1 rounded-xl bg-shell p-1">
        {TABS.map((tab) => {
          const count = todo.filter((row) => kindOf(row) === tab.mode).length;
          return (
            <button key={tab.mode} type="button" role="tab" aria-selected={mode === tab.mode} onClick={() => setMode(tab.mode)}
              className={cx("rounded-[7px] px-2.5 py-1 text-xs font-semibold transition-colors duration-200",
                mode === tab.mode ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
              {tab.label}{rows && <span className="ml-1 tabular-nums text-muted">{count}</span>}
            </button>
          );
        })}
      </div>
      {/* Cao đúng 5 dòng ở mọi trạng thái; nhiều hơn 5 bài thì cuộn trong vùng này. */}
      <div className="-mx-2 h-[256px] overflow-y-auto overscroll-contain px-2">
      {error ? <p role="alert" className="flex h-full items-center justify-center text-center text-[13px] text-danger">Không tải được bài tập.</p>
        : !rows ? <p role="status" className="flex h-full items-center justify-center text-center text-[13px] text-muted">Đang tải bài tập…</p>
        : shown.length === 0 ? <p className="flex h-full items-center justify-center text-center text-[13px] text-muted">{TABS.find((tab) => tab.mode === mode)!.empty}</p>
        : (
          <ul className="-my-1 flex flex-col">
            {shown.map((row) => (
              <li key={row.assignmentId} className="border-b border-line last:border-b-0">
                <Link href={`/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(classId)}`}
                  className="-mx-2 flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-shell">
                  <DeadlineChip closesAt={row.closesAt} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13.5px] font-semibold text-ink">{row.title}</span>
                    <span className="font-mono text-xs text-muted">{row.closesAt ? formatDateTime(row.closesAt) : "Không giới hạn"}</span>
                  </span>
                  <Pill tone={rowStatus(row).tone}>{rowStatus(row).label}</Pill>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Section>
  );
}
