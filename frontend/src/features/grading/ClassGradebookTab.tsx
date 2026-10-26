"use client";
import Link from "next/link";
import { useCallback, useState } from "react";
import { Button, Pill, type Tone } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points, statusLabels } from "./types";
import { GradebookExportDialog } from "./GradebookExportDialog";

function statusTone(status: string): Tone {
  if (status === "PUBLISHED") return "success";
  if (status === "FINALIZED") return "primary";
  if (status === "DRAFT") return "info";
  if (status === "PENDING") return "warning";
  return "neutral";
}

function actionLabel(status: string) {
  if (status === "PENDING") return "Chấm bài";
  if (status === "DRAFT") return "Tiếp tục chấm";
  return "Xem điểm";
}

export function ClassGradebookTab({ classId }: { classId: string }) {
  return <GradebookContent key={classId} classId={classId} />;
}

function GradebookContent({ classId }: { classId: string }) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [mode, setMode] = useState<"assignment" | "matrix">("assignment");
  const [selectedAssignmentId, setSelectedAssignmentId] = useState("");
  const [exporting, setExporting] = useState(false);

  const load = useCallback(
    () => gradingApi.gradebook(classId, query, page),
    [classId, query, page],
  );
  const resource = useResource(load);
  const data = resource.value;
  const assignments = data?.assignments ?? [];
  const activeAssignment =
    assignments.find((item) => item.id === selectedAssignmentId) ?? assignments[0] ?? null;
  const activeAssignmentId = activeAssignment?.id ?? "";

  return (
    <section className="flex min-w-0 flex-col gap-4">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="font-display text-[26px] leading-tight tracking-[-0.02em] text-ink">Sổ điểm</h2>
          <p className="mt-1 text-[13px] text-muted">
            {data
              ? `${data.total} sinh viên · ${assignments.length} bài tính điểm`
              : resource.error
                ? "Không tải được dữ liệu sổ điểm"
                : "Đang tải…"}
          </p>
        </div>
        <button
          type="button"
          disabled={!data || assignments.length === 0}
          onClick={() => setExporting(true)}
          className="inline-flex h-9 items-center gap-2 rounded-lg border border-line-strong bg-surface px-3.5 text-[13px] font-semibold text-ink transition-colors hover:bg-sunken active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-45"
        >
          Xuất sổ điểm
        </button>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-2.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <label className="flex items-center gap-2 text-xs font-semibold text-body">
            <span>Tìm sinh viên</span>
            <input
              type="search"
              className="w-52 max-w-full rounded-lg border border-line-strong bg-surface px-2.5 py-1.5 text-sm font-normal text-ink"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(1);
              }}
              placeholder="Tên hoặc email"
            />
          </label>

          <div
            role="group"
            aria-label="Chế độ xem sổ điểm"
            className="inline-flex rounded-lg border border-line bg-shell p-0.5"
          >
            <button
              type="button"
              aria-pressed={mode === "assignment"}
              onClick={() => setMode("assignment")}
              className={`rounded-md px-2.5 py-1 text-xs font-semibold transition-colors ${
                mode === "assignment"
                  ? "bg-surface text-primary-ink shadow-xs"
                  : "text-muted hover:text-ink"
              }`}
            >
              Theo bài tập
            </button>
            <button
              type="button"
              aria-pressed={mode === "matrix"}
              onClick={() => setMode("matrix")}
              className={`rounded-md px-2.5 py-1 text-xs font-semibold transition-colors ${
                mode === "matrix"
                  ? "bg-surface text-primary-ink shadow-xs"
                  : "text-muted hover:text-ink"
              }`}
            >
              Tổng hợp
            </button>
          </div>
        </div>

        {mode === "assignment" && assignments.length > 0 && (
          <label className="flex min-w-0 flex-1 items-center justify-end gap-2 text-xs font-semibold text-body sm:flex-initial">
            <span className="shrink-0">Bài tập</span>
            <select
              aria-label="Bài tập"
              value={activeAssignmentId}
              onChange={(e) => setSelectedAssignmentId(e.target.value)}
              className="max-w-full min-w-[200px] truncate rounded-lg border border-line-strong bg-surface px-2.5 py-1.5 text-sm font-normal text-ink sm:max-w-[380px]"
            >
              {assignments.map((assignment) => (
                <option key={assignment.id} value={assignment.id}>
                  {assignment.title}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {resource.error && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-danger-line bg-danger-soft px-3.5 py-2.5 text-sm text-danger"
        >
          <span>{resource.error}</span>
          <Button size="sm" onClick={() => void resource.refresh()}>
            Thử tải lại
          </Button>
        </div>
      )}

      {data && assignments.length === 0 && (
        <div className="rounded-xl border border-line bg-surface p-6 text-sm text-muted">
          Chưa có bài tính điểm trong lớp.
        </div>
      )}

      {data && assignments.length > 0 && mode === "assignment" && activeAssignment && (
        <div className="space-y-2.5">
          <div className="flex flex-wrap items-start justify-between gap-2 rounded-xl border border-line bg-surface px-3.5 py-2.5">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <Pill tone="neutral">
                  {activeAssignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}
                </Pill>
                <span className="text-xs text-muted">
                  Thang điểm {Number(activeAssignment.maxScore).toFixed(2)}
                </span>
              </div>
              <h3 className="mt-1 break-words text-sm font-bold text-ink">
                <Link
                  className="text-primary-ink hover:underline"
                  href={`/teaching/classes/${classId}/assignments/${activeAssignment.id}/submissions`}
                >
                  {activeAssignment.title}
                </Link>
              </h3>
            </div>
            <Link
              className="shrink-0 text-xs font-semibold text-primary-ink hover:underline"
              href={`/teaching/classes/${classId}/assignments/${activeAssignment.id}/submissions`}
            >
              Xem bài nộp →
            </Link>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-[10.5px] font-semibold tracking-[.08em] text-muted uppercase">
                <tr>
                  <th scope="col" className="p-3">
                    Sinh viên
                  </th>
                  <th scope="col" className="p-3">
                    Điểm
                  </th>
                  <th scope="col" className="p-3">
                    Trạng thái
                  </th>
                  <th scope="col" className="p-3">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.rows.map((row) => {
                  const grade = row.grades.find(
                    (cell) => cell.assignmentId === activeAssignment.id,
                  );
                  const status = grade?.status ?? "NOT_SUBMITTED";
                  return (
                    <tr key={row.student.accountId} className="border-t border-line">
                      <td className="p-3">
                        <strong className="break-words text-ink">{row.student.name}</strong>
                        <p className="text-xs text-muted">{row.student.email}</p>
                      </td>
                      <td className="p-3 font-semibold whitespace-nowrap tabular-nums text-ink">
                        {points(
                          grade?.score ?? null,
                          grade?.maxScore ?? activeAssignment.maxScore,
                        )}
                      </td>
                      <td className="p-3">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <Pill tone={statusTone(status)}>{statusLabels[status] ?? status}</Pill>
                          {grade?.late && <Pill tone="danger">Trễ</Pill>}
                        </div>
                      </td>
                      <td className="p-3 whitespace-nowrap">
                        {grade?.evaluationId ? (
                          <Link
                            href={`/teaching/evaluations/${grade.evaluationId}`}
                            className="font-semibold text-primary-ink hover:underline"
                          >
                            {actionLabel(status)}
                          </Link>
                        ) : (
                          <span className="text-muted">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {!data.rows.length && (
              <p className="p-6 text-sm text-muted">Không có sinh viên phù hợp.</p>
            )}
          </div>
        </div>
      )}

      {data && assignments.length > 0 && mode === "matrix" && (
        <div className="space-y-2.5">
          <details className="rounded-xl border border-line bg-surface px-3.5 py-2.5 text-xs">
            <summary className="cursor-pointer font-semibold text-primary-ink">
              Danh mục bài tập ({assignments.length})
            </summary>
            <ol className="mt-2 space-y-1.5 pl-4">
              {assignments.map((assignment, index) => (
                <li key={assignment.id} className="break-words text-ink">
                  <span className="font-semibold">Bài {index + 1}:</span>{" "}
                  <Link
                    className="text-primary-ink hover:underline"
                    href={`/teaching/classes/${classId}/assignments/${assignment.id}/submissions`}
                  >
                    {assignment.title}
                  </Link>{" "}
                  <span className="text-muted">
                    ({assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"} · Thang{" "}
                    {Number(assignment.maxScore).toFixed(2)})
                  </span>
                </li>
              ))}
            </ol>
          </details>

          <div className="max-h-[65vh] overflow-auto rounded-xl border border-line bg-surface">
            <table className="w-max min-w-full border-separate border-spacing-0 text-left text-sm">
              <thead>
                <tr>
                  <th
                    scope="col"
                    className="sticky top-0 left-0 z-30 w-[220px] min-w-[220px] max-w-[220px] border-b border-r border-line bg-sunken p-3 text-xs font-semibold uppercase tracking-wide text-muted"
                  >
                    Sinh viên
                  </th>
                  {assignments.map((assignment, index) => (
                    <th
                      key={assignment.id}
                      scope="col"
                      className="sticky top-0 z-20 w-[160px] min-w-[160px] max-w-[160px] border-b border-r border-line bg-sunken p-2.5 align-top last:border-r-0"
                    >
                      <div className="text-[11px] font-bold uppercase tracking-wide text-muted">
                        Bài {index + 1}
                      </div>
                      <Link
                        className="mt-0.5 line-clamp-2 break-words text-xs font-semibold text-primary-ink hover:underline"
                        href={`/teaching/classes/${classId}/assignments/${assignment.id}/submissions`}
                      >
                        {assignment.title}
                      </Link>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((row) => (
                  <tr key={row.student.accountId}>
                    <th
                      scope="row"
                      className="sticky left-0 z-10 w-[220px] min-w-[220px] max-w-[220px] border-b border-r border-line bg-surface p-3 align-top font-normal"
                    >
                      <strong className="block break-words text-ink">{row.student.name}</strong>
                      <p className="break-all text-xs text-muted">{row.student.email}</p>
                    </th>
                    {assignments.map((assignment) => {
                      const grade = row.grades.find(
                        (cell) => cell.assignmentId === assignment.id,
                      );
                      const status = grade?.status ?? "NOT_SUBMITTED";
                      return (
                        <td
                          key={assignment.id}
                          className="w-[160px] min-w-[160px] max-w-[160px] border-b border-r border-line bg-surface p-2.5 align-top last:border-r-0"
                        >
                          {grade?.evaluationId ? (
                            <Link
                              href={`/teaching/evaluations/${grade.evaluationId}`}
                              className="font-semibold whitespace-nowrap tabular-nums text-primary-ink hover:underline"
                            >
                              {grade.score == null
                                ? actionLabel(status)
                                : points(grade.score, grade.maxScore)}
                            </Link>
                          ) : (
                            <span className="whitespace-nowrap tabular-nums text-muted">
                              {points(
                                grade?.score ?? null,
                                grade?.maxScore ?? assignment.maxScore,
                              )}
                            </span>
                          )}
                          <div className="mt-1 flex flex-wrap items-center gap-1">
                            <Pill tone={statusTone(status)}>{statusLabels[status] ?? status}</Pill>
                            {grade?.late && <Pill tone="danger">Trễ</Pill>}
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
            {!data.rows.length && (
              <p className="p-6 text-sm text-muted">Không có sinh viên phù hợp.</p>
            )}
          </div>
        </div>
      )}

      {data && (
        <Pagination
          page={page - 1}
          pageSize={20}
          total={data.total}
          onPageChange={(p) => setPage(p + 1)}
        />
      )}

      <p className="text-xs text-muted">
        Chỉ bài tính điểm và lượt được chấm. Không cộng tổng điểm hoặc trọng số; không bao gồm bài luyện tập
        và Quiz.
      </p>

      {exporting && data && (
        <GradebookExportDialog
          classId={classId}
          assignments={data.assignments}
          onClose={() => setExporting(false)}
        />
      )}
    </section>
  );
}
