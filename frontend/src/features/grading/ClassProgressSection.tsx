"use client";

import Link from "next/link";
import { useCallback, useMemo, useState } from "react";
import { assignmentsApi } from "@/features/assignments/api";
import { Button, Pill } from "@/shared/ui";
import { gradingApi } from "./api";
import type { GradebookPage, SubmissionPage } from "./types";
import { useResource } from "./useResource";

type GradebookAssignment = GradebookPage["assignments"][number];

const INITIAL_VISIBLE_ASSIGNMENTS = 5;

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

export function ClassProgressSection({
  classId,
  layout = "standalone",
}: {
  classId: string;
  layout?: "standalone" | "overview";
}) {
  const [viewState, setViewState] = useState<{
    classId: string;
    showAll: boolean;
    distributionOpen: boolean;
    distributionAssignmentId: string | null;
  }>({
    classId,
    showAll: false,
    distributionOpen: false,
    distributionAssignmentId: null,
  });

  const scopedState =
    viewState.classId === classId
      ? viewState
      : {
          classId,
          showAll: false,
          distributionOpen: false,
          distributionAssignmentId: null,
        };

  const load = useCallback(async () => {
    const [progress, gradebook, openAssignments] = await Promise.all([
      gradingApi.progress(classId),
      gradingApi.gradebook(classId),
      assignmentsApi.listForClass(classId, { status: ["OPEN"] }),
    ]);
    const today = new Date().toLocaleDateString("vi-VN");
    const dueToday = openAssignments.items.filter(
      (assignment) =>
        assignment.closesAt &&
        new Date(assignment.closesAt).toLocaleDateString("vi-VN") === today,
    ).length;
    const pendingTotal = Object.values(progress).reduce(
      (total, page) => total + (page.summary.PENDING ?? 0),
      0,
    );
    return {
      progress,
      assignments: gradebook.assignments,
      openCount: openAssignments.items.length,
      hiddenOpenCount: openAssignments.hiddenCount ?? 0,
      dueToday,
      pendingTotal,
    };
  }, [classId]);

  const { value, error, refresh } = useResource(load);

  const sortedAssignments = useMemo(
    () => (value ? sortAssignmentsForProgress(value.assignments, value.progress) : []),
    [value],
  );

  const visibleAssignments = scopedState.showAll
    ? sortedAssignments
    : sortedAssignments.slice(0, INITIAL_VISIBLE_ASSIGNMENTS);

  const eligibleDistributionAssignments = useMemo(
    () =>
      sortedAssignments.filter(
        (assignment) => (value?.progress[assignment.id]?.summary.PUBLISHED ?? 0) > 0,
      ),
    [sortedAssignments, value],
  );

  const activeDistributionAssignmentId = useMemo(() => {
    if (eligibleDistributionAssignments.length === 0) return null;
    if (
      scopedState.distributionAssignmentId &&
      eligibleDistributionAssignments.some((a) => a.id === scopedState.distributionAssignmentId)
    ) {
      return scopedState.distributionAssignmentId;
    }
    return eligibleDistributionAssignments[0]?.id ?? null;
  }, [eligibleDistributionAssignments, scopedState.distributionAssignmentId]);

  const openCard = (
    <div
      data-testid="overview-open-card"
      data-class-overview-region={layout === "overview" ? "open" : undefined}
      className="flex flex-col justify-between rounded-2xl border border-line bg-white p-4"
    >
      <dt className="text-xs font-semibold text-muted">Bài đang mở</dt>
      {value ? (
        <>
          <dd className="mt-1 text-xl font-bold text-ink">{value.openCount}</dd>
          <dd className="mt-0.5 text-xs text-muted">
            {value.dueToday} bài hết hạn hôm nay
            {value.hiddenOpenCount > 0 ? ` · ${value.hiddenOpenCount} bài ẩn theo quyền` : ""}
          </dd>
        </>
      ) : error ? (
        <dd className="mt-1 text-sm font-semibold text-danger">Không khả dụng</dd>
      ) : (
        <dd className="mt-1 text-sm font-medium text-muted">Đang tải…</dd>
      )}
    </div>
  );

  const pendingCard = (
    <div
      data-testid="overview-pending-card"
      data-class-overview-region={layout === "overview" ? "pending" : undefined}
      className="flex flex-col justify-between rounded-2xl border border-line bg-white p-4"
    >
      <dt className="text-xs font-semibold text-muted">Chờ chấm</dt>
      {value ? (
        <>
          <dd className="mt-1 text-xl font-bold text-ink">{value.pendingTotal}</dd>
          <dd className="mt-0.5 text-xs text-muted">Bài nộp chờ chấm</dd>
        </>
      ) : error ? (
        <dd className="mt-1 text-sm font-semibold text-danger">Không khả dụng</dd>
      ) : (
        <dd className="mt-1 text-sm font-medium text-muted">Đang tải…</dd>
      )}
    </div>
  );

  const progressPanel = (
    <section
      data-testid="overview-progress-panel"
      data-class-overview-region={layout === "overview" ? "progress" : undefined}
      className={
        layout === "overview"
          ? "rounded-2xl border border-line bg-white p-5 min-w-0"
          : "mt-5 rounded-xl border border-line bg-white p-5 min-w-0"
      }
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-base font-bold text-ink">Tiến độ nộp bài</h2>
        {sortedAssignments.length > INITIAL_VISIBLE_ASSIGNMENTS && (
          <Button
            size="sm"
            variant="secondary"
            aria-expanded={scopedState.showAll}
            onClick={() =>
              setViewState({
                ...scopedState,
                showAll: !scopedState.showAll,
              })
            }
          >
            {scopedState.showAll ? "Thu gọn" : `Xem tất cả (${sortedAssignments.length})`}
          </Button>
        )}
      </div>

      {layout === "standalone" && (
        <dl className="my-4 grid gap-3 sm:grid-cols-2">
          {openCard}
          {pendingCard}
        </dl>
      )}

      {error && (
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          <p role="alert">{error}</p>
          <Button size="sm" variant="secondary" onClick={() => void refresh().catch(() => {})}>
            Thử tải lại
          </Button>
        </div>
      )}

      {!value && !error && (
        <p role="status" className="mt-3 text-sm text-muted">
          Đang tải tiến độ nộp bài…
        </p>
      )}

      {value && sortedAssignments.length === 0 && (
        <p className="mt-3 text-sm text-muted">Chưa có bài Graded trong lớp.</p>
      )}

      {value && visibleAssignments.length > 0 && (
        <div className="mt-3 divide-y divide-line rounded-xl border border-line">
          {visibleAssignments.map((assignment) => {
            const p = value.progress[assignment.id];
            const total = Math.max(0, p?.summary.total ?? 0);
            const submitted = Math.max(0, p?.summary.submitted ?? 0);
            const pendingCount = p?.summary.PENDING ?? 0;
            const percent = total > 0 ? Math.round((100 * submitted) / total) : 0;
            const unitLabel = assignment.submissionMode === "GROUP" ? "nhóm" : "sinh viên";
            const submissionHref = `/teaching/classes/${classId}/assignments/${assignment.id}/submissions`;
            return (
              <div
                key={assignment.id}
                data-testid="progress-assignment-row"
                data-assignment-id={assignment.id}
                className="flex flex-col gap-2 p-3.5"
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <Link
                      className="font-semibold text-primary hover:underline break-words"
                      href={submissionHref}
                    >
                      {assignment.title}
                    </Link>
                    <p className="mt-0.5 text-xs text-muted">
                      {assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"} · Graded ·{" "}
                      {p?.deadline
                        ? `Hạn: ${new Date(p.deadline).toLocaleString("vi-VN")}`
                        : "Không có hạn nộp"}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    {pendingCount > 0 && <Pill tone="warning">{pendingCount} chờ chấm</Pill>}
                    <Link
                      className="text-xs font-semibold text-primary hover:underline whitespace-nowrap"
                      href={submissionHref}
                    >
                      Xem bài nộp
                    </Link>
                  </div>
                </div>
                <p className="text-xs text-ink">
                  {p
                    ? `${submitted} / ${total} ${unitLabel} đã nộp (${percent}%) · ${pendingCount} chờ chấm`
                    : "Chưa có dữ liệu"}
                </p>
                {p && (
                  <progress
                    className="h-2 w-full overflow-hidden rounded-full accent-primary [&::-webkit-progress-bar]:rounded-full [&::-webkit-progress-bar]:bg-slate-100 [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-primary [&::-moz-progress-bar]:bg-primary"
                    aria-label={`Tiến độ nộp ${assignment.title}`}
                    max={Math.max(1, total)}
                    value={Math.min(submitted, Math.max(1, total))}
                  />
                )}
              </div>
            );
          })}
        </div>
      )}

      {value && sortedAssignments.length > 0 && (
        <div className="mt-4 border-t border-line pt-4">
          <button
            type="button"
            aria-expanded={scopedState.distributionOpen}
            className="flex w-full items-center justify-between rounded-lg border border-line bg-slate-50 px-3.5 py-2.5 text-left text-sm font-semibold text-ink hover:bg-slate-100"
            onClick={() =>
              setViewState({
                ...scopedState,
                distributionOpen: !scopedState.distributionOpen,
              })
            }
          >
            <span>Phân bố điểm đã công bố</span>
            <span className="text-xs font-medium text-muted">
              {scopedState.distributionOpen ? "Thu gọn" : "Mở phân bố điểm"}
            </span>
          </button>

          {scopedState.distributionOpen && (
            <div className="mt-3 rounded-xl border border-line bg-slate-50/60 p-3.5">
              {eligibleDistributionAssignments.length === 0 ? (
                <p className="text-sm text-muted">
                  Chưa đủ dữ liệu công bố hoặc ẩn theo ngưỡng riêng tư để hiển thị phân bố điểm.
                </p>
              ) : (
                <>
                  <div className="flex flex-wrap items-center gap-2">
                    <label
                      htmlFor={`dist-assignment-${classId}`}
                      className="text-xs font-semibold text-muted"
                    >
                      Bài tập đã công bố
                    </label>
                    <select
                      id={`dist-assignment-${classId}`}
                      aria-label="Bài tập xem phân bố điểm"
                      className="max-w-full rounded-lg border border-line bg-white px-3 py-1.5 text-sm text-ink"
                      value={activeDistributionAssignmentId ?? ""}
                      onChange={(e) =>
                        setViewState({
                          ...scopedState,
                          distributionAssignmentId: e.target.value,
                        })
                      }
                    >
                      {eligibleDistributionAssignments.map((assignment) => (
                        <option key={assignment.id} value={assignment.id}>
                          {assignment.title}
                        </option>
                      ))}
                    </select>
                  </div>
                  {activeDistributionAssignmentId && (
                    <PublishedDistribution
                      key={`${classId}:${activeDistributionAssignmentId}`}
                      classId={classId}
                      assignmentId={activeDistributionAssignmentId}
                    />
                  )}
                </>
              )}
            </div>
          )}
        </div>
      )}
    </section>
  );

  if (layout === "overview") {
    return (
      <>
        {openCard}
        {pendingCard}
        {progressPanel}
      </>
    );
  }

  return progressPanel;
}

function PublishedDistribution({
  classId,
  assignmentId,
}: {
  classId: string;
  assignmentId: string;
}) {
  const load = useCallback(
    () => gradingApi.classDistribution(classId, assignmentId),
    [classId, assignmentId],
  );
  const { value, error, refresh } = useResource(load);
  return (
    <section className="mt-3">
      <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">
        Phân bố điểm đã công bố
      </h3>
      {error && (
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-red-200 bg-red-50 p-2.5 text-sm text-red-800">
          <p role="alert">{error}</p>
          <Button size="sm" variant="secondary" onClick={() => void refresh().catch(() => {})}>
            Thử tải lại
          </Button>
        </div>
      )}
      {!value && !error && (
        <p role="status" className="mt-2 text-sm text-muted">
          Đang tải phân bố điểm…
        </p>
      )}
      {value?.length === 0 && (
        <p className="mt-2 text-sm text-muted">
          Chưa đủ dữ liệu công bố hoặc ẩn theo ngưỡng riêng tư để hiển thị phân bố điểm.
        </p>
      )}
      {value && value.length > 0 && (
        <ul className="mt-2 space-y-2">
          {value.map((bucket) => (
            <li key={bucket.label} className="flex items-center gap-3 text-sm">
              <span className="w-20 shrink-0 font-medium text-ink">{bucket.label}</span>
              <progress
                className="h-2 flex-1 overflow-hidden rounded-full accent-primary [&::-webkit-progress-bar]:rounded-full [&::-webkit-progress-bar]:bg-slate-200 [&::-webkit-progress-value]:rounded-full [&::-webkit-progress-value]:bg-primary [&::-moz-progress-bar]:bg-primary"
                aria-label={`Mức điểm ${bucket.label}`}
                max={Math.max(1, ...value.map((item) => item.count))}
                value={bucket.count}
              />
              <span className="shrink-0 text-xs text-muted">{bucket.count} sinh viên</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
