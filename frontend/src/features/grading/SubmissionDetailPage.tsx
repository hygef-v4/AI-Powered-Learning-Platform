"use client";
import Link from "next/link";
import { useCallback, useState } from "react";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { points, statusLabels } from "./types";
import { Pagination } from "@/shared/ui/Pagination";
import { BatchActionDialog } from "./BatchActionDialog";
import { Alert, Button, EmptyState, Icon, Pill, Select, type Tone } from "@/shared/ui";
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

const CELL = "border-b border-line px-3 py-3 align-middle first:pl-1 last:pr-1";
const HEAD = "border-b border-line px-3 pb-2.5 text-left text-[10.5px] font-semibold tracking-[.08em] whitespace-nowrap text-muted uppercase first:pl-1 last:pr-1";

/** Bài nộp của một bài tập: số liệu nhanh, lọc, chọn nhiều để chốt/công bố/AI, bảng dạng tài liệu. */
export function SubmissionDetailPage({
  classId,
  assignmentId,
}: {
  classId: string;
  assignmentId: string;
}) {
  const context = useSubmissionContext(classId, assignmentId);
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
  const selectable = data?.rows.filter((r) => r.evaluationId) ?? [];
  return (
    <main className="flex min-w-0 flex-col gap-6">
      <header className="flex animate-rise flex-col gap-2.5">
        <Link className="inline-flex w-fit items-center gap-1.5 text-[12.5px] font-semibold text-muted transition-colors hover:text-ink" href={`/teaching/classes/${classId}`}>
          <Icon name="arrow_back" size={14} />Về lớp học
        </Link>
        <h1 className="font-display text-[34px] leading-[1.1] tracking-[-0.025em] break-words text-ink [overflow-wrap:anywhere] sm:text-[40px]">
          {context.value ? `Bài nộp — ${context.value.title.trim() || "Bài tập chưa có tiêu đề"}` : "Bài nộp"}
        </h1>
        {context.value ? (
          <p className="text-[13.5px] break-words text-muted [overflow-wrap:anywhere]">
            {context.value.ownerName.trim() ? `Lớp: ${context.value.ownerName}` : "Chưa có thông tin lớp"}
          </p>
        ) : context.error ? (
          <div role="alert" className="flex items-center gap-3 text-[13.5px] text-muted">
            <p>{context.error}</p>
            <button
              type="button"
              className="font-semibold text-primary-ink"
              data-testid="submissions-context-retry"
              aria-label="Thử tải lại thông tin bài tập/lớp"
              onClick={() => void context.refresh()}
            >
              Thử lại
            </button>
          </div>
        ) : (
          <p role="status" className="text-[13.5px] text-muted">Đang tải thông tin bài tập/lớp.</p>
        )}
      </header>
      {resource.error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void resource.refresh().catch(() => {})}>Thử lại</Button>}>
          {resource.error}
        </Alert>
      )}
      {!data && !resource.error && <p role="status" className="text-sm text-muted">Đang tải bài nộp…</p>}
      {data && (
        <>
          <div className="flex flex-wrap gap-x-10 gap-y-4 border-b border-line pb-5">
            {[
              ["submitted", "Đã nộp"],
              ["inProgress", "Đang làm"],
              ["notSubmitted", "Chưa nộp"],
              ["late", "Nộp trễ"],
            ].map(([key, label]) => (
              <section key={key} className="flex flex-col gap-1">
                <strong className="font-display text-[32px] leading-none font-normal text-ink tabular-nums">{data.summary[key!] ?? 0}</strong>
                <p className="text-[10.5px] font-semibold tracking-[.08em] text-muted uppercase">{label}</p>
              </section>
            ))}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select
              aria-label="Trạng thái"
              data-testid="submissions-status-filter"
              wrapperClassName="w-48"
              value={filter}
              onChange={(e) => {
                setFilter(e.target.value);
                setPage(1);
                setSelected([]);
              }}
            >
              <option value="ALL">Mọi trạng thái</option>
              {["PENDING", "DRAFT", "FINALIZED", "PUBLISHED", "IN_PROGRESS", "NOT_SUBMITTED"].map((s) => (
                <option key={s} value={s}>
                  {statusLabels[s]}
                </option>
              ))}
            </Select>
            <Select
              aria-label="Nộp bài"
              data-testid="submissions-late-filter"
              wrapperClassName="w-40"
              value={late}
              onChange={(e) => {
                setLate(e.target.value);
                setPage(1);
                setSelected([]);
              }}
            >
              <option value="">Đúng hạn và trễ</option>
              <option value="false">Đúng hạn</option>
              <option value="true">Nộp trễ</option>
            </Select>
            {items.length > 0 && (
              <div className="ml-auto flex animate-pop flex-wrap items-center gap-2 rounded-lg bg-primary-soft py-1 pr-1 pl-3">
                <span className="text-[13px] font-semibold text-ink">Đã chọn {items.length} bài</span>
                {(["ai", "finalize", "publish"] as const).map((a) => (
                  <Button key={a} size="sm" variant={a === "publish" ? "primary" : "secondary"} onClick={() => setAction(a)}>
                    {a === "ai" ? "AI đề xuất" : a === "finalize" ? "Chốt điểm" : "Công bố điểm"}
                  </Button>
                ))}
                <Button size="sm" variant="ghost" onClick={() => setSelected([])}>
                  Bỏ chọn
                </Button>
              </div>
            )}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-[13.5px] [&_tbody_tr:last-child>td]:border-b-0">
              <thead>
                <tr>
                  <th className={HEAD}>
                    <input
                      aria-label="Chọn tất cả bài trong trang"
                      type="checkbox"
                      className="accent-[var(--c-primary)]"
                      checked={selectable.length > 0 && selectable.every((r) => selected.includes(r.evaluationId!))}
                      onChange={(e) => setSelected(e.target.checked ? selectable.map((r) => r.evaluationId!) : [])}
                    />
                  </th>
                  {["Sinh viên / nhóm", "Nộp lúc", "Lượt", "Trạng thái", "Điểm", ""].map((s, i) => (
                    <th key={i} className={HEAD}>
                      {s}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.rows.map((r) => (
                  <tr key={r.student.accountId} className="transition-colors duration-200 hover:bg-surface">
                    <td className={CELL}>
                      <input
                        aria-label={`Chọn ${r.student.name}`}
                        disabled={!r.evaluationId}
                        type="checkbox"
                        className="accent-[var(--c-primary)]"
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
                    <td className={CELL}>
                      <strong className="font-semibold break-words text-ink">{r.student.name}</strong>
                      <p className="font-mono text-xs break-all text-muted">{r.student.email}</p>
                    </td>
                    <td className={`${CELL} font-mono text-xs whitespace-nowrap text-body`}>
                      {r.submittedAt ? new Date(r.submittedAt).toLocaleString("vi-VN") : "—"}
                      {r.late && <span className="ml-2 font-sans font-semibold text-danger">Trễ</span>}
                    </td>
                    <td className={`${CELL} tabular-nums`}>{r.attemptCount}</td>
                    <td className={CELL}>
                      <Pill tone={STATUS_TONES[r.status] ?? "neutral"}>{statusLabels[r.status] ?? r.status}</Pill>
                    </td>
                    <td className={`${CELL} font-semibold whitespace-nowrap tabular-nums`}>{points(r.score, r.maxScore)}</td>
                    <td className={`${CELL} text-right whitespace-nowrap`}>
                      {r.evaluationId ? (
                        <Link
                          className="inline-flex items-center gap-1 font-semibold text-primary-ink transition-colors hover:text-ink"
                          href={`/teaching/evaluations/${r.evaluationId}`}
                        >
                          {rowActionLabel(r.status)}
                          <Icon name="chevron_right" size={13} />
                        </Link>
                      ) : r.submittedAt ? (
                        <span className="text-xs text-muted">Đánh giá chưa sẵn sàng</span>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!data.rows.length && <EmptyState title="Không có bài nộp phù hợp." />}
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
