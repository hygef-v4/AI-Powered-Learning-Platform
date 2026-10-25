"use client";
import Link from "next/link";
import { useCallback, useId, useState } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points, statusLabels } from "./types";
import { Pagination } from "@/shared/ui/Pagination";
import { BatchActionDialog } from "./BatchActionDialog";
import { Field, Pill, Select, type Tone } from "@/shared/ui";
import { useSubmissionContext } from "./useSubmissionContext";

const STATUS_TONES: Record<string, Tone> = {
  PENDING: "warning",
  DRAFT: "info",
  FINALIZED: "primary",
  PUBLISHED: "success",
  SUPERSEDED: "neutral",
  NOT_SUBMITTED: "neutral",
  IN_PROGRESS: "neutral",
};

function rowActionLabel(status: string): string {
  if (status === "PENDING") return "Chấm bài";
  if (status === "DRAFT") return "Tiếp tục chấm";
  if (status === "FINALIZED" || status === "PUBLISHED" || status === "SUPERSEDED") return "Xem điểm";
  return "Chấm / xem";
}

export function SubmissionDetailPage({
  classId,
  assignmentId,
}: {
  classId: string;
  assignmentId: string;
}) {
  const context = useSubmissionContext(classId, assignmentId);
  const filterId = useId();
  const [filter, setFilter] = useState("ALL");
  const [late, setLate] = useState("");
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<string[]>([]);
  const [action, setAction] = useState<"ai" | "finalize" | "publish" | null>(null);
  const load = useCallback(
    () => gradingApi.submissions(classId, assignmentId, filter, page, late),
    [classId, assignmentId, filter, page, late],
  );
  const resource = useResource(load);
  const data = resource.value;
  const items =
    data?.rows
      .filter((r) => r.evaluationId && selected.includes(r.evaluationId))
      .map((r) => ({ evaluationId: r.evaluationId!, version: r.version })) ?? [];
  return (
    <main className="min-w-0 space-y-5">
      <Link className="text-primary" href={`/teaching/classes/${classId}`}>
        ← Về lớp học
      </Link>
      <header>
        <h1 className="break-words text-2xl font-bold [overflow-wrap:anywhere]">
          {context.value ? `Bài nộp — ${context.value.title.trim() || "Bài tập chưa có tiêu đề"}` : "Bài nộp"}
        </h1>
        {context.value ? (
          <p className="mt-2 break-words text-slate-600 [overflow-wrap:anywhere]">
            {context.value.ownerName.trim() ? `Lớp: ${context.value.ownerName}` : "Chưa có thông tin lớp"}
          </p>
        ) : context.error ? (
          <div role="alert" className="mt-2 text-slate-600">
            <p>{context.error}</p>
            <button
              type="button"
              className="text-primary"
              data-testid="submissions-context-retry"
              aria-label="Thử tải lại thông tin bài tập/lớp"
              onClick={() => void context.refresh()}
            >
              Thử lại
            </button>
          </div>
        ) : (
          <p role="status" className="mt-2 text-slate-600">Đang tải thông tin bài tập/lớp.</p>
        )}
        <p className="mt-2 text-slate-600">
          Bài Practice và Quiz không có trong danh sách chấm điểm này.
        </p>
      </header>
      {resource.error && (
        <p role="alert">
          {resource.error}{" "}
          <button onClick={() => void resource.refresh().catch(() => {})}>Thử lại</button>
        </p>
      )}
      {!data && !resource.error && <p role="status">Đang tải bài nộp…</p>}
      {data && (
        <>
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
            {[
              ["submitted", "Đã nộp"],
              ["inProgress", "Đang làm"],
              ["notSubmitted", "Chưa nộp"],
              ["late", "Nộp trễ"],
            ].map(([key, label]) => (
              <section key={key} className="flex items-center justify-between gap-2 rounded-xl border border-line bg-white px-3.5 py-2.5">
                <p className="text-xs font-medium text-muted">{label}</p>
                <strong className="text-lg font-bold tabular-nums text-ink">{data.summary[key!] ?? 0}</strong>
              </section>
            ))}
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Field label="Trạng thái" htmlFor={`${filterId}-status`}>
              <Select
                id={`${filterId}-status`}
                data-testid="submissions-status-filter"
                value={filter}
                onChange={(e) => {
                  setFilter(e.target.value);
                  setPage(1);
                  setSelected([]);
                }}
              >
                <option value="ALL">Tất cả</option>
                {["PENDING", "DRAFT", "FINALIZED", "PUBLISHED", "IN_PROGRESS", "NOT_SUBMITTED"].map(
                  (s) => (
                    <option key={s} value={s}>
                      {statusLabels[s]}
                    </option>
                  ),
                )}
              </Select>
            </Field>
            <Field label="Nộp bài" htmlFor={`${filterId}-late`}>
              <Select
                id={`${filterId}-late`}
                data-testid="submissions-late-filter"
                value={late}
                onChange={(e) => {
                  setLate(e.target.value);
                  setPage(1);
                  setSelected([]);
                }}
              >
                <option value="">Tất cả</option>
                <option value="false">Đúng hạn</option>
                <option value="true">Nộp trễ</option>
              </Select>
            </Field>
            {items.length > 0 ? (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-medium text-ink">Đã chọn {items.length} bài</span>
                {(["ai", "finalize", "publish"] as const).map((a) => (
                  <button
                    key={a}
                    type="button"
                    onClick={() => setAction(a)}
                    className="rounded-lg border border-line-strong bg-white px-3 py-1.5 text-xs font-semibold text-ink hover:bg-shell"
                  >
                    {a === "ai" ? "AI đề xuất" : a === "finalize" ? "Chốt điểm" : "Công bố điểm"}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => setSelected([])}
                  className="text-xs font-medium text-muted underline underline-offset-2 hover:text-ink"
                >
                  Bỏ chọn
                </button>
              </div>
            ) : (
              <span className="text-xs text-muted">Đã chọn 0 bài · Tích chọn bài nộp để thao tác hàng loạt</span>
            )}
          </div>
          <div className="overflow-x-auto rounded-xl border border-line bg-white">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="p-3">
                    <input
                      aria-label="Chọn tất cả bài trong trang"
                      type="checkbox"
                      checked={
                        data.rows.some((r) => r.evaluationId) &&
                        data.rows
                          .filter((r) => r.evaluationId)
                          .every((r) => selected.includes(r.evaluationId!))
                      }
                      onChange={(e) =>
                        setSelected(
                          e.target.checked
                            ? data.rows.flatMap((r) => (r.evaluationId ? [r.evaluationId] : []))
                            : [],
                        )
                      }
                    />
                  </th>
                  {["Sinh viên / nhóm", "Nộp lúc", "Lượt", "Trạng thái", "Điểm", ""].map((s, i) => (
                    <th key={i} className="p-3">
                      {s}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((r) => (
                  <tr key={r.student.accountId} className="border-t border-line">
                    <td className="p-3">
                      <input
                        aria-label={`Chọn ${r.student.name}`}
                        disabled={!r.evaluationId}
                        type="checkbox"
                        checked={!!r.evaluationId && selected.includes(r.evaluationId)}
                        onChange={(e) =>
                          setSelected(
                            e.target.checked
                              ? [...selected, r.evaluationId!]
                              : selected.filter((id) => id !== r.evaluationId),
                          )
                        }
                      />
                    </td>
                    <td className="p-3">
                      <strong className="break-words">{r.student.name}</strong>
                      <p className="break-all text-xs text-slate-500">{r.student.email}</p>
                    </td>
                    <td className="p-3 whitespace-nowrap">
                      {r.submittedAt ? new Date(r.submittedAt).toLocaleString("vi-VN") : "—"}
                      {r.late && <span className="ml-2 text-red-600">Trễ</span>}
                    </td>
                    <td className="p-3 tabular-nums">{r.attemptCount}</td>
                    <td className="p-3">
                      <Pill tone={STATUS_TONES[r.status] ?? "neutral"}>{statusLabels[r.status] ?? r.status}</Pill>
                    </td>
                    <td className="p-3 whitespace-nowrap font-semibold tabular-nums">{points(r.score, r.maxScore)}</td>
                    <td className="p-3 whitespace-nowrap">
                      {r.evaluationId ? (
                        <Link
                          className="font-medium text-primary hover:underline"
                          href={`/teaching/evaluations/${r.evaluationId}`}
                        >
                          {rowActionLabel(r.status)}
                        </Link>
                      ) : r.submittedAt ? (
                        "Đánh giá chưa sẵn sàng"
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!data.rows.length && <p className="p-6">Không có bài nộp phù hợp.</p>}
          </div>
          <Pagination
            page={page - 1}
            pageSize={20}
            total={data.total}
            onPageChange={(n) => {
              setPage(n + 1);
              setSelected([]);
            }}
          />
        </>
      )}
      {action && (
        <BatchActionDialog
          action={action}
          items={items}
          onClose={() => setAction(null)}
          onDone={() => void resource.refresh().catch(() => {})}
        />
      )}
    </main>
  );
}
