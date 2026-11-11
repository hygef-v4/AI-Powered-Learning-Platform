"use client";

import Link from "next/link";
import { useCallback } from "react";
import { Button, Section } from "@/shared/ui";
import { formatDateTime } from "@/shared/dateTime";
import { assignmentsApi } from "@/features/assignments/api";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";

/** Shared entry point from class summaries and submission workspaces. */
export function buildGradebookHref(classId: string, pick?: { assignment?: string; student?: string }) {
  const params = new URLSearchParams({ tab: "gradebook" });
  if (pick?.assignment) params.set("assignment", pick.assignment);
  if (pick?.student) params.set("student", pick.student);
  return `/teaching/classes/${encodeURIComponent(classId)}?${params}`;
}

export function ClassProgressSection({ classId }: { classId: string; layout?: "standalone" | "overview" }) {
  const load = useCallback(async () => {
    const [assignments, progress] = await Promise.all([
      assignmentsApi.listForClass(classId, { status: ["OPEN"] }),
      gradingApi.loadSubmissionProgress(classId),
    ]);
    return { assignments: assignments.items.filter(row => row.status === "OPEN"), progress, loadedAt: Date.now() };
  }, [classId]);
  const { value, error, refresh } = useResource(load);
  const assignmentsHref = `/teaching/classes/${encodeURIComponent(classId)}?tab=evals&assignmentState=open`;
  const pending = Object.values(value?.progress ?? {}).reduce((sum, page) => sum + (page.summary.PENDING ?? 0), 0);
  const unpublished = Object.values(value?.progress ?? {}).reduce(
    (sum, page) => sum + (page.summary.DRAFT ?? 0) + (page.summary.FINALIZED ?? 0), 0,
  );
  const pendingAssignment = Object.entries(value?.progress ?? {}).find(([, page]) => (page.summary.PENDING ?? 0) > 0)?.[0];
  const unpublishedAssignment = Object.entries(value?.progress ?? {}).find(
    ([, page]) => (page.summary.DRAFT ?? 0) + (page.summary.FINALIZED ?? 0) > 0,
  )?.[0];
  const upcoming = (value?.assignments ?? [])
    .filter(row => row.closesAt && Date.parse(row.closesAt) > (value?.loadedAt ?? 0))
    .sort((left, right) => Date.parse(left.closesAt!) - Date.parse(right.closesAt!))
    .slice(0, 3);

  return (
    <Section title="Tổng quan bài tập">
      {error ? (
        <div className="flex flex-col items-start gap-3">
          <p role="alert" className="text-sm text-danger">{error}</p>
          <Button size="sm" variant="secondary" onClick={() => void refresh().catch(() => {})}>Thử tải lại</Button>
        </div>
      ) : !value ? (
        <p role="status" className="text-sm text-muted">Đang tải tổng quan…</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            {[
              { label: "Bài đang mở", count: value.assignments.length, href: assignmentsHref },
              { label: "Bài nộp chờ chấm", count: pending, href: `${buildGradebookHref(classId, { assignment: pendingAssignment })}&gradebookStatus=PENDING` },
              { label: "Bài đã chấm chưa công bố", count: unpublished, href: `${buildGradebookHref(classId, { assignment: unpublishedAssignment })}&gradebookStatus=DRAFT` },
            ].map(item => (
              <Link key={item.label} href={item.href} aria-label={`${item.count} ${item.label}`} className="row-interactive flex items-baseline gap-3 rounded-lg px-2 py-3 hover:bg-shell">
                <span className="text-2xl font-semibold tabular-nums text-ink">{item.count}</span>
                <span className="text-sm text-body">{item.label}</span>
              </Link>
            ))}
          </div>
          <div className="border-t border-line pt-5">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-ink">Hạn nộp sắp tới</h3>
              <Link href={assignmentsHref} className="text-[13px] font-semibold text-primary-ink hover:underline">Tất cả bài tập</Link>
            </div>
            {upcoming.length === 0 ? <p className="text-sm text-muted">Chưa có hạn nộp sắp tới.</p> : (
              <ul className="divide-y divide-line">
                {upcoming.map(assignment => (
                  <li key={assignment.id} data-testid="upcoming-assignment">
                    <Link href={`/assignments/${encodeURIComponent(assignment.id)}?classId=${encodeURIComponent(classId)}`}
                      className="row-interactive -mx-2 flex flex-wrap items-center justify-between gap-x-6 gap-y-2 rounded-lg px-2 py-3 hover:bg-shell">
                      <span className="min-w-0 flex-1 basis-48">
                        <span className="block break-words text-sm font-semibold text-ink">{assignment.title}</span>
                        <span className="text-xs text-muted">{assignment.gradingMode === "PRACTICE" ? "Luyện tập" : assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}</span>
                      </span>
                      <time dateTime={assignment.closesAt!} className="text-[13px] tabular-nums text-muted">{formatDateTime(assignment.closesAt)}</time>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </Section>
  );
}
