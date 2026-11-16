"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { fetchApi } from "@/shared/api/client";
import { AutoTextarea, type RubricCriterion } from "@/shared/document";
import { Card, Icon, TotalScoreCard } from "@/shared/ui";
import { GradingFeedbackSummary } from "./StructuredFeedbackSection";
import { matchesPersonQuery } from "@/shared/studentCode";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import type {
  GradingFormValues,
  Rubric,
  StructuredFeedbackSummary,
  SubmissionRow,
} from "../model/types";
import type { SubmissionDocumentDto } from "../model/submissionDocument";
import { msg } from "@/i18n/msg";

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
          const token = await fetchApi<{ url: string }>(
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

export function listNoteCriteria(rubric: Rubric): RubricCriterion[] {
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

/** Thẻ Tổng điểm tạm tính bên lề trang chấm; dáng chung từ `TotalScoreCard`. */
export function TotalCard({ total, maxScore }: { total: number; maxScore: number }) {
  return (
    <TotalScoreCard score={total} maxScore={maxScore} ariaLabel={msg("grading.components.GradingWorkspaceHeader.diemTamTinh")}
      format={(value) => (value ?? 0).toFixed(2)} />
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
      className="sticky top-14 z-30 main-bleed -mt-8 flex items-center justify-between gap-3 border-b border-line bg-[var(--c-topbar)] py-2.5 backdrop-blur-md md:-mt-10"
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
  studentCode,
  directory,
  onSelect,
}: {
  evaluationId: string;
  busy: boolean;
  studentName: string;
  studentEmail: string;
  /** MSSV của người nộp (BR-U09-75); rỗng với bài nhóm hoặc sinh viên chưa có mã. */
  studentCode?: string;
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
      matchesPersonQuery(row.student, search),
  );

  return (
    <div ref={dropdownRef} className="relative">
      <button
        type="button"
        aria-label={msg("grading.components.GradingWorkspaceHeader.chonSinhVien")}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="flex max-w-[min(18rem,40vw)] cursor-pointer items-center gap-1.5 rounded-md border border-line bg-surface px-2.5 py-1 text-left transition-colors hover:bg-sunken"
      >
        <span className="min-w-0 flex-1 truncate">
          <span className="block text-xs font-semibold leading-tight text-ink">{studentName}</span>
          <PersonSubline className="leading-tight break-normal truncate" studentCode={studentCode} email={studentEmail} />
        </span>
        <Icon name="unfold_more" size={14} className="shrink-0 text-muted" />
      </button>
      {open && (
        <Card padding="sm" className="absolute left-1/2 top-full z-20 mt-2 w-[min(22rem,calc(100vw-3rem))] -translate-x-1/2 shadow-[var(--shadow-float)]">
          <label className="flex flex-col gap-2 text-sm font-semibold text-ink">
            {msg("grading.components.GradingWorkspaceHeader.timSinhVien")}<input
              type="search"
              placeholder={msg("grading.components.GradingWorkspaceHeader.tenEmailHoacMssv")}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="w-full rounded-lg border border-field px-3 py-2 text-sm font-normal"
            />
          </label>
          {directory.error ? (
            <p role="alert" className="mt-3 text-sm text-danger">
              {msg("grading.components.GradingWorkspaceHeader.khongTaiDuocDanhSach")}{" "}
              <button type="button" className="underline" onClick={() => void directory.refresh().catch(() => {})}>
                {msg("grading.components.GradingWorkspaceHeader.thuLai")}</button>
            </p>
          ) : !directory.value ? (
            <p role="status" className="mt-3 text-sm text-muted">
              {msg("grading.components.GradingWorkspaceHeader.dangTaiSinhVien")}</p>
          ) : (
            <ul aria-label={msg("grading.components.GradingWorkspaceHeader.baiNopSinhVien")} className="mt-2 max-h-64 overflow-y-auto">
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
                    <PersonSubline studentCode={row.student.studentCode} email={row.student.email} />
                  </button>
                </li>
              ))}
              {!matches.length && <li className="py-3 text-sm text-muted">{msg("grading.components.GradingWorkspaceHeader.khongCoBaiNopPhuHop")}</li>}
            </ul>
          )}
        </Card>
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
        <p className="text-xs text-muted">{msg("grading.components.GradingWorkspaceHeader.nguoiThucHien")}{unanchoredAuthors.join(", ")}</p>
      )}
      <section aria-label={msg("grading.components.GradingWorkspaceHeader.nhanXetChung")} className="mt-1 text-sm leading-relaxed text-body">
        <label htmlFor="grading-general-feedback" className="text-sm font-semibold text-ink">{msg("grading.components.GradingWorkspaceHeader.nhanXetChung")}</label>
        <AutoTextarea
          id="grading-general-feedback"
          aria-label={msg("grading.components.GradingWorkspaceHeader.nhanXetChung")}
          disabled={disabled}
          rows={3}
          maxLength={10000}
          value={form.feedback}
          onChange={(event) => setFeedback(event.target.value)}
          placeholder={msg("grading.components.GradingWorkspaceHeader.nhapNhanXetChungChoBai")}
          className="mt-2 min-h-28 rounded-lg border! border-line bg-surface! p-3! text-sm font-normal leading-6 text-ink focus:border-focus disabled:opacity-60"
        />
        {form.feedback.trim() && <p className="sr-only">{form.feedback}</p>}
      </section>
      <div className="flex flex-wrap items-end gap-3">
        <label className="block text-xs font-semibold text-ink">
          {msg("grading.components.GradingWorkspaceHeader.diemCongTru")}<input
            type="number"
            step="0.25"
            aria-label={msg("grading.components.GradingWorkspaceHeader.diemCongTru")}
            disabled={disabled}
            value={form.adjustment}
            onChange={(event) => setAdjustment(event.target.value)}
            className="mt-1 block w-28 rounded-lg border border-line bg-surface px-2.5 py-1 text-xs font-normal text-ink focus:border-focus focus:outline-none disabled:opacity-60"
          />
        </label>
      </div>
      {!disabled && <details className="text-xs text-muted">
        <summary className="cursor-pointer">{msg("grading.components.GradingWorkspaceHeader.ghiChuThayDoiTuyChon")}</summary>
        <input aria-label={msg("grading.components.GradingWorkspaceHeader.ghiChuThayDoi")} value={form.changeReason}
          onChange={event => setChangeReason(event.target.value)}
          className="mt-2 block w-full rounded-lg border border-line bg-surface px-2.5 py-1 text-xs font-normal text-ink focus:border-focus focus:outline-none" />
      </details>}
      <GradingFeedbackSummary
        summary={form.structuredFeedback?.summary}
        title={msg("grading.components.GradingWorkspaceHeader.diemManhVaDiemCanCai")}
        editable={!disabled}
        disabled={disabled}
        onChange={updateStructuredSummary}
      />
    </div>
  );
}
