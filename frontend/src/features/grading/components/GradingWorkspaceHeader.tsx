"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { apiFetch } from "@/shared/api/client";
import { AutoTextarea, type RubricCriterion } from "@/shared/document";
import { Icon } from "@/shared/ui";
import { GradingFeedbackSummary } from "./StructuredFeedbackSection";
import type {
  GradingFormValues,
  Rubric,
  StructuredFeedbackSummary,
  SubmissionRow,
} from "../model/types";
import type { SubmissionDocumentDto } from "../model/submissionDocument";

/** Link tải có hạn cho từng ảnh trong bài nộp (U03 qua U09), lấy một lần khi mở bài. */
export function useSubmissionImages(
  evaluationId: string,
  document: SubmissionDocumentDto,
): Record<string, string> {
  const [urls, setUrls] = useState<Record<string, string>>({});
  useEffect(() => {
    let active = true;
    const images = document.blocks.filter(
      (block): block is Extract<SubmissionDocumentDto["blocks"][number], { type: "IMAGE" }> =>
        block.type === "IMAGE",
    );
    void Promise.all(
      images.map(async (block): Promise<[string, string] | null> => {
        if (!block.content.fileId) return null;
        try {
          const token = await apiFetch<{ url: string }>(
            `/api/v1/evaluations/${evaluationId}/images/${encodeURIComponent(block.content.fileId)}`,
          );
          const parsed = new URL(token.url, window.location.origin);
          return ["https:", "http:"].includes(parsed.protocol) ? [block.id, parsed.href] : null;
        } catch {
          return null;
        }
      }),
    ).then((entries) => {
      if (active) {
        setUrls(Object.fromEntries(entries.filter((entry): entry is [string, string] => entry !== null)));
      }
    });
    return () => {
      active = false;
    };
  }, [evaluationId, document]);
  return urls;
}

export function noteCriteria(rubric: Rubric): RubricCriterion[] {
  return rubric.criteria.map((criterion) => ({
    criterionId: criterion.criterionId,
    title: criterion.title,
    description: "",
    kind: "TEXT",
    items: criterion.items.map((item) => ({
      itemId: item.itemId,
      text: item.text,
      points: Number(item.points),
    })),
  }));
}

function Meter({ awarded, max }: { awarded: number | null; max: number | null }) {
  const ratio = awarded != null && max ? Math.min(1, Math.max(0, awarded / max)) : 0;
  return (
    <span aria-hidden className="mt-2.5 block h-1 overflow-hidden rounded-full bg-sunken">
      <span className="block h-full rounded-full bg-primary" style={{ width: `${ratio * 100}%` }} />
    </span>
  );
}

export function TotalCard({ total, maxScore }: { total: number; maxScore: number }) {
  return (
    <article className="rounded-xl border border-line bg-surface px-4 py-3.5 shadow-[var(--shadow-lift)]">
      <p className="text-xs font-medium text-muted">Tổng điểm</p>
      <p aria-label="Điểm tạm tính" className="mt-1 flex items-baseline gap-1.5 tabular-nums">
        <span className="font-display text-[30px] leading-none tracking-[-0.02em] text-ink">
          {total.toFixed(2)}
        </span>
        <span className="text-[13px] text-muted"> / {maxScore.toFixed(2)}</span>
      </p>
      <Meter awarded={total} max={maxScore} />
    </article>
  );
}

export function TopBar({
  barRef,
  left,
  setViewSlot,
  actions,
}: {
  barRef: (element: HTMLElement | null) => void;
  left: ReactNode;
  setViewSlot: (element: HTMLElement | null) => void;
  actions?: ReactNode;
}) {
  return (
    <header
      ref={barRef}
      className="sticky top-14 z-30 -mx-4 -mt-8 flex items-center justify-between gap-3 border-b border-line bg-[var(--c-topbar)] px-4 py-2.5 backdrop-blur-md sm:-mx-8 sm:px-8 md:-mt-10"
    >
      <div className="flex min-w-0 shrink items-center gap-x-2.5">{left}</div>
      <div ref={setViewSlot} data-testid="view-slot" className="flex shrink-0 justify-center" />
      <div className="flex shrink-0 items-center justify-end gap-2 whitespace-nowrap">{actions}</div>
    </header>
  );
}

export function StudentPickerDropdown({
  evaluationId,
  busy,
  studentName,
  studentEmail,
  directory,
  onSelect,
}: {
  evaluationId: string;
  busy: boolean;
  studentName: string;
  studentEmail: string;
  directory: { value: SubmissionRow[] | null; error: string; refresh: () => Promise<unknown> };
  onSelect: (targetEvaluationId: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    window.addEventListener("mousedown", handleOutside);
    return () => window.removeEventListener("mousedown", handleOutside);
  }, [open]);

  const matches = (directory.value ?? []).filter(
    (row) =>
      row.evaluationId &&
      `${row.student.name} ${row.student.email}`
        .toLocaleLowerCase("vi")
        .includes(search.trim().toLocaleLowerCase("vi")),
  );

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        aria-label="Chọn sinh viên"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="flex max-w-[min(18rem,40vw)] cursor-pointer items-center gap-1.5 rounded-md border border-line bg-surface px-2.5 py-1 text-left transition-colors hover:bg-sunken"
      >
        <span className="min-w-0 flex-1 truncate">
          <span className="block text-xs font-semibold leading-tight text-ink">{studentName}</span>
          {studentEmail && <span className="block text-xs leading-tight text-muted">{studentEmail}</span>}
        </span>
        <Icon name="unfold_more" size={14} className="shrink-0 text-muted" />
      </button>
      {open && (
        <div className="absolute left-1/2 top-full z-20 mt-2 w-[min(22rem,calc(100vw-3rem))] -translate-x-1/2 rounded-xl border border-line bg-surface p-3 shadow-[var(--shadow-float)]">
          <label className="flex flex-col gap-2 text-sm font-semibold text-ink">
            Tìm sinh viên
            <input
              type="search"
              placeholder="Tên hoặc email"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="w-full rounded-lg border border-field px-3 py-2 text-sm font-normal"
            />
          </label>
          {directory.error ? (
            <p role="alert" className="mt-3 text-sm text-danger">
              Không tải được danh sách.{" "}
              <button type="button" className="underline" onClick={() => void directory.refresh().catch(() => {})}>
                Thử lại
              </button>
            </p>
          ) : !directory.value ? (
            <p role="status" className="mt-3 text-sm text-muted">
              Đang tải sinh viên…
            </p>
          ) : (
            <ul aria-label="Bài nộp sinh viên" className="mt-2 max-h-64 overflow-y-auto">
              {matches.map((row) => (
                <li key={row.student.accountId}>
                  <button
                    type="button"
                    disabled={busy || row.evaluationId === evaluationId}
                    onClick={() => {
                      onSelect(row.evaluationId!);
                      setOpen(false);
                    }}
                    className="w-full cursor-pointer rounded-lg px-2 py-2 text-left text-sm hover:bg-sunken disabled:cursor-default disabled:opacity-50"
                  >
                    <span className="block font-semibold">{row.student.name}</span>
                    <span className="block text-xs text-muted">{row.student.email}</span>
                  </button>
                </li>
              ))}
              {!matches.length && <li className="py-3 text-sm text-muted">Không có bài nộp phù hợp.</li>}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export function GradingHeaderSection({
  assignmentKind,
  unanchoredAuthors,
  form,
  disabled,
  setFeedback,
  setAdjustment,
  setChangeReason,
  updateStructuredSummary,
}: {
  assignmentKind: string;
  unanchoredAuthors: string[];
  form: GradingFormValues;
  disabled: boolean;
  setFeedback: (value: string) => void;
  setAdjustment: (value: string) => void;
  setChangeReason: (value: string) => void;
  updateStructuredSummary: (summary: StructuredFeedbackSummary) => void;
}) {
  return (
    <div className="mb-5 flex flex-col gap-2.5 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {assignmentKind}
      </p>
      {unanchoredAuthors.length > 0 && (
        <p className="text-xs text-muted">Người thực hiện: {unanchoredAuthors.join(", ")}</p>
      )}
      <section aria-label="Nhận xét chung" className="mt-1 text-sm leading-relaxed text-body">
        <label htmlFor="grading-general-feedback" className="text-sm font-semibold text-ink">Nhận xét chung</label>
        <AutoTextarea
          id="grading-general-feedback"
          aria-label="Nhận xét chung"
          disabled={disabled}
          rows={3}
          maxLength={10000}
          value={form.feedback}
          onChange={(event) => setFeedback(event.target.value)}
          placeholder="Nhập nhận xét chung cho bài làm…"
          className="mt-2 min-h-28 rounded-lg border! border-line bg-surface! p-3! text-sm font-normal leading-6 text-ink focus:border-focus disabled:opacity-60"
        />
        {form.feedback.trim() && <p className="sr-only">{form.feedback}</p>}
      </section>
      <div className="flex flex-wrap items-end gap-3">
        <label className="block text-xs font-semibold text-ink">
          Điểm cộng/trừ
          <input
            type="number"
            step="0.25"
            aria-label="Điểm cộng/trừ"
            disabled={disabled}
            value={form.adjustment}
            onChange={(event) => setAdjustment(event.target.value)}
            className="mt-1 block w-28 rounded-lg border border-line bg-surface px-2.5 py-1 text-xs font-normal text-ink focus:border-focus focus:outline-none disabled:opacity-60"
          />
        </label>
      </div>
      {!disabled && <details className="text-xs text-muted">
        <summary className="cursor-pointer">Ghi chú thay đổi (tùy chọn)</summary>
        <input aria-label="Ghi chú thay đổi" value={form.changeReason}
          onChange={event => setChangeReason(event.target.value)}
          className="mt-2 block w-full rounded-lg border border-line bg-surface px-2.5 py-1 text-xs font-normal text-ink focus:border-focus focus:outline-none" />
      </details>}
      <GradingFeedbackSummary
        summary={form.structuredFeedback?.summary}
        title="Điểm mạnh và điểm cần cải thiện"
        editable={!disabled}
        disabled={disabled}
        onChange={updateStructuredSummary}
      />
    </div>
  );
}

export function AiProgressModal({ open }: { open: boolean }) {
  if (!open || typeof document === "undefined") return null;
  return createPortal(
    <div className="fixed inset-0 z-[100] flex animate-fade items-center justify-center bg-[var(--c-scrim)] p-4 backdrop-blur-[2px]">
      <div
        role="alertdialog"
        aria-modal="true"
        aria-busy="true"
        aria-label="Đang chấm bằng AI"
        data-testid="ai-progress"
        className="flex animate-pop items-center gap-3 rounded-xl border border-line bg-surface px-6 py-5 shadow-[var(--shadow-float)]"
      >
        <span
          aria-hidden="true"
          className="size-5 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none"
        />
        <span className="text-[14px] font-semibold text-ink">Đang chấm bằng AI…</span>
      </div>
    </div>,
    document.body,
  );
}
