"use client";

import Link from "next/link";
import { useState } from "react";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { Section, Segmented } from "@/shared/ui";
import { DeadlineChip } from "./DeadlineChip";
import type { StudentRow } from "../model/types";
import { isDoing, isUpcoming, useStudentAssignments } from "../hooks/useStudentAssignments";
import { msg } from "@/i18n/msg";

type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TABS: { mode: Kind; label: string; empty: string }[] = [
  { mode: "INDIVIDUAL", label: msg("assignmentwork.components.ClassTodoCard.baiCaNhan"), empty: msg("assignmentwork.components.ClassTodoCard.khongConBaiCaNhanCan") },
  { mode: "GROUP", label: msg("assignmentwork.components.ClassTodoCard.baiNhom"), empty: msg("assignmentwork.components.ClassTodoCard.khongConBaiNhomCanLam") },
  { mode: "PRACTICE", label: msg("assignmentwork.components.ClassTodoCard.luyenTap"), empty: msg("assignmentwork.components.ClassTodoCard.khongConBaiLuyenTapCan") },
];

/** Loại bài như trên bảng Bài tập: bài luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function classifyKind(row: StudentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/**
 * Tổng quan lớp dùng cùng tiêu chí ba tab Bài tập; danh sách cần làm giữ lọc loại bài và hạn gần nhất.
 */
export function ClassTodoCard({ classId }: { classId: string }) {
  const { rows, error } = useStudentAssignments(classId);
  const [mode, setMode] = useUrlState<Kind>("todoKind", "INDIVIDUAL", ["INDIVIDUAL", "GROUP", "PRACTICE"]);
  // Mốc "còn hạn" lấy một lần lúc mở trang.
  const [now] = useState(() => Date.now());
  const todo = (rows ?? [])
    .filter((row) => isUpcoming(row) && (!row.closesAt || new Date(row.closesAt).getTime() > now))
    .sort((a, b) => (a.closesAt ? new Date(a.closesAt).getTime() : Infinity) - (b.closesAt ? new Date(b.closesAt).getTime() : Infinity));
  const shown = todo.filter((row) => classifyKind(row) === mode);
  const assignmentsHref = `/my-assignments?classId=${encodeURIComponent(classId)}`;
  return (
    <Section title={msg("assignmentwork.components.ClassTodoCard.tongQuanBaiTap")}>
      {rows && !error && <div className="grid gap-4 sm:grid-cols-3">
        {[
          { label: msg("assignmentwork.components.ClassTodoCard.chuaLam"), tab: "upcoming", count: rows.filter(row => isUpcoming(row) && !isDoing(row)).length },
          { label: msg("assignmentwork.components.ClassTodoCard.dangLam"), tab: "doing", count: rows.filter(isDoing).length },
          { label: msg("assignmentwork.components.ClassTodoCard.daNop"), tab: "submitted", count: rows.filter(row => row.submittedCount > 0 || row.group?.submitted).length },
        ].map(item => <Link key={item.tab} href={`${assignmentsHref}&tab=${item.tab}`}
          aria-label={`${item.count} ${item.label}`}
          className="row-interactive flex items-baseline gap-3 rounded-lg px-2 py-3 hover:bg-shell">
          <span className="text-2xl font-semibold tabular-nums text-ink">{item.count}</span>
          <span className="text-sm text-body">{item.label}</span>
        </Link>)}
      </div>}
      <div className="flex flex-col gap-4 border-t border-line pt-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-ink">{msg("assignmentwork.components.ClassTodoCard.baiTapCanLam")}</h3>
          <Link href={assignmentsHref} className="text-[13px] font-semibold text-primary-ink hover:underline">{msg("assignmentwork.components.ClassTodoCard.tatCaBaiTap")}</Link>
        </div>
        <Segmented
          as="tab"
          ariaLabel={msg("assignmentwork.components.ClassTodoCard.loaiBai")}
          className="-mt-1 w-fit"
          value={mode}
          onChange={setMode}
          options={TABS.map((tab) => ({
            value: tab.mode,
            label: tab.label,
            count: rows ? todo.filter((row) => classifyKind(row) === tab.mode).length : undefined,
          }))}
        />
        {/* Tối đa5 dòng; ít bài thì thẻ co theo nội dung. */}
        <div className="-mx-2 max-h-[256px] overflow-x-hidden overflow-y-auto px-2 [scrollbar-color:var(--c-muted)_transparent] [scrollbar-gutter:stable]">
        {error ? <p role="alert" className="py-6 text-center text-[13px] text-danger">{msg("assignmentwork.components.ClassTodoCard.khongTaiDuocBaiTap")}</p>
          : !rows ? <p role="status" className="py-6 text-center text-[13px] text-muted">{msg("assignmentwork.components.ClassTodoCard.dangTaiBaiTap")}</p>
          : shown.length === 0 ? <p className="py-6 text-center text-[13px] text-muted">{TABS.find((tab) => tab.mode === mode)!.empty}</p>
          : (
            <ul className="flex flex-col">
              {shown.map((row) => (
                <li key={row.assignmentId} className="border-b border-line last:border-b-0">
                  <Link href={`/my-assignments/${encodeURIComponent(row.assignmentId)}?classId=${encodeURIComponent(classId)}`}
                    className="row-interactive -mx-2 flex items-center gap-3 rounded-lg px-2 py-3 hover:bg-shell">
                    <span className="min-w-0 flex-1 truncate text-[13.5px] font-semibold text-ink">{row.title}</span>
                    <DeadlineChip closesAt={row.closesAt} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Section>
  );
}
