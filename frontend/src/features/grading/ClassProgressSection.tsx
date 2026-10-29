"use client";

import Link from "next/link";
import { useCallback, useMemo, useState } from "react";
import { Button, Section, Segmented } from "@/shared/ui";
import { gradingApi } from "./api";
import type { GradebookPage, SubmissionPage } from "./types";
import { useResource } from "./useResource";

type GradebookAssignment = GradebookPage["assignments"][number];

function parseDeadlineMs(deadline: string | null | undefined): number | null {
  if (!deadline) return null;
  const parsed = Date.parse(deadline);
  return Number.isNaN(parsed) ? null : parsed;
}

function sortAssignmentsForProgress(
  assignments: GradebookAssignment[],
  progress: Record<string, SubmissionPage>,
): GradebookAssignment[] {
  return [...assignments].sort((left, right) => {
    const leftPending = (progress[left.id]?.summary.PENDING ?? 0) > 0 ? 1 : 0;
    const rightPending = (progress[right.id]?.summary.PENDING ?? 0) > 0 ? 1 : 0;
    if (leftPending !== rightPending) {
      return rightPending - leftPending;
    }
    const leftDeadline = parseDeadlineMs(progress[left.id]?.deadline);
    const rightDeadline = parseDeadlineMs(progress[right.id]?.deadline);
    if (leftDeadline !== null && rightDeadline !== null && leftDeadline !== rightDeadline) {
      return leftDeadline - rightDeadline;
    }
    if (leftDeadline !== null && rightDeadline === null) return -1;
    if (leftDeadline === null && rightDeadline !== null) return 1;
    return left.id.localeCompare(right.id);
  });
}

/** Đường dẫn sang tab Chấm điểm của lớp, mở sẵn một bài tập hoặc một sinh viên. */
export function gradebookHref(classId: string, pick?: { assignment?: string; student?: string }) {
  const params = new URLSearchParams({ tab: "gradebook" });
  if (pick?.assignment) params.set("assignment", pick.assignment);
  if (pick?.student) params.set("student", pick.student);
  return `/teaching/classes/${encodeURIComponent(classId)}?${params}`;
}

/** Lượt đã nộp (đang chờ chấm hoặc đã chấm), không tính Chưa nộp và Đang làm. */
const SUBMITTED = new Set(["PENDING", "DRAFT", "FINALIZED", "PUBLISHED"]);

type View = "assignment" | "student";

/** Cả lớp trong sổ điểm (trang 100 sinh viên), để tính tiến độ theo từng sinh viên. */
async function wholeGradebook(classId: string) {
  const first = await gradingApi.gradebook(classId, "", 1, 100);
  const rows = [...first.rows];
  for (let page = 2; rows.length < first.total && page <= 50; page++) {
    const next = await gradingApi.gradebook(classId, "", page, 100);
    if (next.rows.length === 0) break;
    rows.push(...next.rows);
  }
  return { ...first, rows };
}

/**
 * Tiến độ nộp bài trên tab Thông tin lớp của giảng viên: tab Theo bài tập (mỗi bài tính điểm một dòng: tên, loại bài, số đã
 * nộp, thanh tiến độ; bài có bài chờ chấm lên trước rồi đến hạn gần nhất) và Theo sinh viên (mỗi sinh viên một dòng:
 * số bài đã nộp trên tổng số bài, người nộp ít lên trước). Bấm một dòng để mở Sổ điểm với bài hoặc sinh viên đó; nút Xem sổ
 * điểm trên đầu thẻ. Danh sách cao cố định, nhiều dòng thì cuộn trong thẻ.
 */
export function ClassProgressSection({ classId }: { classId: string; layout?: "standalone" | "overview" }) {
  const [view, setView] = useState<View>("assignment");
  const load = useCallback(async () => {
    const [progress, gradebook] = await Promise.all([gradingApi.progress(classId), wholeGradebook(classId)]);
    return { progress, assignments: gradebook.assignments, rows: gradebook.rows };
  }, [classId]);

  const { value, error, refresh } = useResource(load);

  const sortedAssignments = useMemo(
    () => (value ? sortAssignmentsForProgress(value.assignments, value.progress) : []),
    [value],
  );
  const students = useMemo(() => (value ? (value.rows ?? []).map((row) => ({
    student: row.student,
    submitted: row.grades.filter((cell) => SUBMITTED.has(cell.status)).length,
  })).sort((a, b) => a.submitted - b.submitted || a.student.name.localeCompare(b.student.name, "vi")) : []), [value]);

  const empty = sortedAssignments.length === 0 || (view === "student" && students.length === 0);

  return (
    <Section title="Tiến độ nộp bài"
      actions={<Link href={gradebookHref(classId)} className="text-[13px] font-semibold text-primary-ink hover:underline">Chấm điểm</Link>}>
      <Segmented as="tab" ariaLabel="Xem tiến độ" value={view} onChange={setView} className="self-start"
        options={[{ value: "assignment", label: "Theo bài tập" }, { value: "student", label: "Theo sinh viên" }]} />
      {/* Cao bằng danh sách Thông báo gần đây bên cạnh; nhiều dòng thì cuộn trong vùng này. */}
      <div className="-mx-2 h-[248px] overflow-x-hidden overflow-y-auto px-2 [scrollbar-color:var(--c-muted)_transparent] [scrollbar-gutter:stable]">
        {error ? (
          <div className="flex h-full flex-col items-center justify-center gap-2 text-center">
            <p role="alert" className="text-[13px] text-danger">{error}</p>
            <Button size="sm" variant="secondary" onClick={() => void refresh().catch(() => {})}>Thử tải lại</Button>
          </div>
        ) : !value ? (
          <p role="status" className="flex h-full items-center justify-center text-[13px] text-muted">Đang tải tiến độ nộp bài…</p>
        ) : empty ? (
          <p className="flex h-full items-center justify-center text-[13px] text-muted">
            {sortedAssignments.length === 0 ? "Chưa có bài tính điểm trong lớp." : "Lớp chưa có sinh viên."}
          </p>
        ) : view === "assignment" ? (
          <ul className="-my-1 flex flex-col">
            {sortedAssignments.map((assignment) => {
              const p = value.progress[assignment.id];
              const total = Math.max(0, p?.summary.total ?? 0);
              const submitted = Math.max(0, p?.summary.submitted ?? 0);
              const unit = assignment.submissionMode === "GROUP" ? "nhóm" : "sinh viên";
              return (
                <ProgressRow key={assignment.id} testId="progress-assignment-row" dataId={assignment.id}
                  href={gradebookHref(classId, { assignment: assignment.id })} title={assignment.title} meta={assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}
                  done={submitted} total={total} hint={`${submitted} / ${total} ${unit} đã nộp`} showBar={!!p} />
              );
            })}
          </ul>
        ) : (
          <ul className="-my-1 flex flex-col">
            {students.map(({ student, submitted }) => (
              <ProgressRow key={student.accountId} testId="progress-student-row" dataId={student.accountId}
                href={gradebookHref(classId, { student: student.accountId })} title={student.name} meta={student.email}
                done={submitted} total={sortedAssignments.length} hint={`Đã nộp ${submitted} / ${sortedAssignments.length} bài`} showBar />
            ))}
          </ul>
        )}
      </div>
    </Section>
  );
}

/** Một dòng tiến độ: tên, dòng phụ, số đã nộp trên tổng và thanh tiến độ; cả dòng là liên kết sang Chấm điểm. */
function ProgressRow({ testId, dataId, href, title, meta, done, total, hint, showBar }: {
  testId: string; dataId: string; href: string; title: string; meta: string; done: number; total: number; hint: string; showBar: boolean;
}) {
  return (
    <li data-testid={testId} data-assignment-id={testId === "progress-assignment-row" ? dataId : undefined} className="border-b border-line last:border-b-0">
      <Link href={href} className="row-interactive -mx-2 flex flex-col gap-1.5 rounded-lg px-2 py-2.5 hover:bg-shell">
        <span className="flex items-center gap-3">
          <span className="min-w-0 flex-1">
            <span className="block truncate text-[13.5px] font-semibold text-ink">{title}</span>
            <span className="block truncate text-xs text-muted">{meta}</span>
          </span>
          <span className="shrink-0 text-xs text-muted tabular-nums" title={hint}>{done}/{total}</span>
        </span>
        {showBar && (
          <span aria-hidden className="block h-1 overflow-hidden rounded-full bg-sunken">
            <span className="block h-full rounded-full bg-primary"
              ref={(node) => { if (node) node.style.width = `${total > 0 ? Math.round((100 * done) / total) : 0}%`; }} />
          </span>
        )}
      </Link>
    </li>
  );
}
