"use client";
import { useState } from "react";
import { Button, Icon, Pill } from "@/shared/ui";
import { AutoTextarea } from "@/shared/document";
import type {
  StructuredFeedbackItem,
  StructuredFeedbackSummary,
} from "../model/types";

function cleanSuggestionText(suggestion: string, isFullMarks: boolean): string {
  if (!isFullMarks) return suggestion;
  return suggestion.replace(/^Gợi ý thêm\s*\(không trừ điểm\)\s*:\s*/i, "");
}

function FeedbackSummaryList({ label, items, disabled, onChange }: {
  label: string;
  items: string[];
  disabled: boolean;
  onChange: (items: string[]) => void;
}) {
  const rows = items.length ? items : [""];
  return <fieldset className="min-w-0">
    <legend className="mb-2 text-sm font-semibold text-ink">{label}</legend>
      <ul className="list-disc space-y-3 pl-5 marker:text-muted">
        {rows.map((item, index) => <li key={index}>
          <div className="flex items-start gap-1.5">
            <AutoTextarea aria-label={`${label}, ý ${index + 1}`} value={item} maxLength={500}
              disabled={disabled} rows={2}
              onChange={(event) => onChange(rows.map((entry, at) => at === index ? event.target.value : entry))}
              className="min-h-16 min-w-0 flex-1 rounded-lg border! border-line bg-surface! px-3! py-2! text-sm font-normal leading-6 text-ink focus:border-focus" />
            {items.length > 0 && <button type="button" aria-label={`Xóa ý ${index + 1}: ${label}`} disabled={disabled}
              onClick={() => onChange(items.filter((_, at) => at !== index))}
              className="mt-1 inline-flex size-8 shrink-0 items-center justify-center rounded-md text-muted hover:bg-danger-soft hover:text-danger focus-visible:outline-2 focus-visible:outline-focus disabled:opacity-40">
              <Icon name="close" size={16} />
            </button>}
          </div>
        </li>)}
      </ul>
    {items.length > 0 && <div className="mt-2 flex items-center justify-between gap-2">
      <Button size="sm" variant="ghost" icon="add" aria-label={`Thêm ý: ${label}`}
        disabled={disabled || items.length >= 5} onClick={() => onChange([...items, ""])}>Thêm ý</Button>
      {items.length >= 4 && <span className="text-xs text-muted">{items.length}/5 ý</span>}
    </div>}
  </fieldset>;
}

export function GradingFeedbackSummary({
  summary,
  title = "Tóm tắt nhận xét cấu trúc",
  editable = false,
  disabled = false,
  onChange,
}: {
  summary?: StructuredFeedbackSummary | null;
  title?: string;
  editable?: boolean;
  disabled?: boolean;
  onChange?: (next: StructuredFeedbackSummary) => void;
}) {
  const hasAny = Boolean(summary?.strengths.length || summary?.improvements.length);
  const [expanded, setExpanded] = useState<boolean>();
  if (editable && onChange) {
    const values = summary ?? { strengths: [], improvements: [], nextSteps: [] };
    return (
      <details open={expanded ?? hasAny} onToggle={(event) => setExpanded(event.currentTarget.open)}
        className="mt-3 border-t border-line-soft pt-3 text-sm leading-6">
        <summary className="cursor-pointer font-semibold text-ink">{title}</summary>
        <div className="mt-4 grid gap-5 md:grid-cols-2">
          <FeedbackSummaryList label="Điểm mạnh" items={values.strengths} disabled={disabled}
            onChange={(strengths) => onChange({ ...values, strengths })} />
          <FeedbackSummaryList label="Điểm cần cải thiện" items={values.improvements} disabled={disabled}
            onChange={(improvements) => onChange({ ...values, improvements })} />
        </div>
      </details>
    );
  }
  if (!summary || !hasAny) return null;

  return (
    <section
      aria-label={title}
      className="mt-3 rounded-xl border border-line bg-shell/40 p-4 text-sm leading-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-ink">{title}</h3>
      </div>
        <div className={`mt-2 grid gap-2.5 ${summary.strengths.length > 0 && summary.improvements.length > 0 ? "sm:grid-cols-2" : ""}`}>
          {summary.strengths.length > 0 && (
            <div className="rounded-lg border border-line-soft bg-surface p-2.5">
              <p className="font-semibold text-success">✓ Làm tốt</p>
              <ul className="mt-2 list-disc space-y-2 pl-5 text-ink">
                {summary.strengths.map((entry, idx) => (
                  <li key={idx} className="whitespace-pre-wrap break-words">{entry}</li>
                ))}
              </ul>
            </div>
          )}
          {summary.improvements.length > 0 && (
            <div className="rounded-lg border border-line-soft bg-surface p-2.5">
              <p className="font-semibold text-warning">! Cần sửa</p>
              <ul className="mt-2 list-disc space-y-2 pl-5 text-ink">
                {summary.improvements.map((entry, idx) => (
                  <li key={idx} className="whitespace-pre-wrap break-words">{entry}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
    </section>
  );
}

export function RubricItemFeedback({
  rubricId,
  itemId,
  itemText,
  awardedPoints,
  maxPoints,
  feedbackItem,
  acknowledged = false,
  validBlockIds,
  fallbackBlockId,
  editable = false,
  disabled = false,
  onChangeItem,
  onToggleAcknowledge,
}: {
  rubricId: string;
  itemId: string;
  itemText: string;
  awardedPoints: number;
  maxPoints: number;
  feedbackItem?: StructuredFeedbackItem | null;
  acknowledged?: boolean;
  validBlockIds?: Set<string> | string[];
  fallbackBlockId?: string;
  editable?: boolean;
  disabled?: boolean;
  onChangeItem?: (updated: StructuredFeedbackItem) => void;
  onToggleAcknowledge?: (acknowledged: boolean) => void;
}) {
  if (!feedbackItem && (!editable || !onChangeItem)) return null;
  const itemFeedback = feedbackItem ?? { rubricId, itemId, evidence: null, reason: "", suggestion: null,
    evidenceBlockId: null, requiresReview: false, reviewReason: null };

  const isFullMarks =
    Number.isFinite(awardedPoints) &&
    Number.isFinite(maxPoints) &&
    maxPoints > 0 &&
    Math.round(awardedPoints * 100) === Math.round(maxPoints * 100);
  const needsAiReview = Boolean(itemFeedback.requiresReview && !acknowledged);
  const showAckControl = editable && Boolean(onToggleAcknowledge) && (itemFeedback.requiresReview || acknowledged);

  const blockId = itemFeedback.evidenceBlockId?.trim() || fallbackBlockId || null;
  const hasValidBlock = blockId
    ? validBlockIds instanceof Set
      ? validBlockIds.has(blockId)
      : Array.isArray(validBlockIds)
        ? validBlockIds.includes(blockId)
        : false
    : false;
  const canNavigateToBlock = Boolean(blockId && hasValidBlock);
  const suggestionLabel = isFullMarks ? "Gợi ý thêm (không trừ điểm)" : "Cách cải thiện";

  return (
    <details
      open={needsAiReview || undefined}
      className={`mt-2 text-xs leading-relaxed ${
        needsAiReview
          ? "rounded-lg border border-warning bg-warning-soft/50 p-2.5 text-ink"
          : "border-t border-line-soft pt-2 text-ink"
      }`}
    >
      <summary className="flex cursor-pointer flex-wrap items-center justify-between gap-2 font-semibold text-ink">
        <span className="inline-flex flex-wrap items-center gap-1.5">
          <span>Nhận xét & minh chứng</span>
          {needsAiReview && (
            <Pill tone="warning">
              <span aria-hidden="true">⚠</span>
              <span>Cần giảng viên kiểm tra</span>
            </Pill>
          )}
          {!needsAiReview && acknowledged && (
            <Pill tone="success">
              <span aria-hidden="true">✓</span>
              <span>Đã xác nhận</span>
            </Pill>
          )}
        </span>
      </summary>

      <div className="mt-2 space-y-2">
        {itemFeedback.requiresReview && (
          <div
            role="note"
            className="rounded-md border border-warning bg-surface px-2.5 py-2 text-xs text-ink"
          >
            <p className="font-semibold text-warning">
              ⚠ Cần giảng viên kiểm tra thủ công:{" "}
              <span className="font-normal text-ink">
                {itemFeedback.reviewReason ||
                  "Minh chứng chưa đọc rõ hoặc có hướng thiết kế thay thế hợp lệ."}
              </span>
            </p>
          </div>
        )}

        {editable && onChangeItem ? (
          <div className="space-y-2">
            <label className="block font-medium text-ink">
              Minh chứng từ bài làm
              <AutoTextarea
                aria-label={`Minh chứng: ${itemText}`}
                disabled={disabled}
                rows={3}
                maxLength={1000}
                value={itemFeedback.evidence ?? ""}
                onChange={(e) =>
                  onChangeItem({ ...itemFeedback, evidence: e.target.value || null })
                }
                className="mt-1 min-h-24 rounded-lg border! border-line bg-surface! p-2! text-sm font-normal leading-relaxed text-ink focus:border-focus"
              />
            </label>
            <label className="block font-medium text-ink">
              Giải thích điểm
              <AutoTextarea
                aria-label={`Giải thích điểm: ${itemText}`}
                disabled={disabled}
                rows={3}
                maxLength={1000}
                value={itemFeedback.reason ?? ""}
                onChange={(e) =>
                  onChangeItem({ ...itemFeedback, reason: e.target.value })
                }
                className="mt-1 min-h-24 rounded-lg border! border-line bg-surface! p-2! text-sm font-normal leading-relaxed text-ink focus:border-focus"
              />
            </label>
            <label className="block font-medium text-ink">
              {suggestionLabel}
              <AutoTextarea
                aria-label={`Gợi ý: ${itemText}`}
                disabled={disabled}
                rows={3}
                maxLength={1000}
                value={itemFeedback.suggestion ?? ""}
                onChange={(e) =>
                  onChangeItem({ ...itemFeedback, suggestion: e.target.value || null })
                }
                className="mt-1 min-h-24 rounded-lg border! border-line bg-surface! p-2! text-sm font-normal leading-relaxed text-ink focus:border-focus"
              />
            </label>
          </div>
        ) : (
          <div className="space-y-1.5">
            {itemFeedback.evidence && (
              <p className="break-words">
                <strong className="text-body">Minh chứng: </strong>
                <span>{itemFeedback.evidence}</span>
              </p>
            )}
            {itemFeedback.reason?.trim() && <p className="break-words">
              <strong className="text-body">Giải thích điểm: </strong>
              <span>{itemFeedback.reason}</span>
            </p>}
            {itemFeedback.suggestion && (
              <p className="break-words">
                <strong className="text-body">{suggestionLabel}: </strong>
                <span>{cleanSuggestionText(itemFeedback.suggestion, isFullMarks)}</span>
              </p>
            )}
          </div>
        )}

        {blockId && (
          <div className="text-xs">
            {canNavigateToBlock ? (
              <a
                href={`#submission-block-${blockId}`}
                onClick={(event) => {
                  // Bài nộp hiện trong trình soạn chỉ đọc: block mang data-block-id, không có id riêng.
                  const target = document.querySelector(`[data-block-id="${CSS.escape(blockId)}"]`);
                  if (!target) return;
                  event.preventDefault();
                  target.scrollIntoView({ behavior: "smooth", block: "center" });
                }}
                className="font-semibold text-primary-ink underline underline-offset-2 hover:text-ink"
              >
                Đi tới đoạn bài làm
              </a>
            ) : (
              <span className="text-muted">
                Đoạn tham chiếu không còn trong bài nộp.
              </span>
            )}
          </div>
        )}

        {showAckControl && onToggleAcknowledge && (
          <label className="mt-1 flex cursor-pointer items-center gap-2 rounded border border-line bg-shell/60 px-2.5 py-1.5 font-medium text-ink">
            <input
              type="checkbox"
              disabled={disabled}
              checked={acknowledged}
              aria-label={`Đã kiểm tra và xác nhận giải thích: ${itemText}`}
              onChange={(e) => onToggleAcknowledge(e.target.checked)}
              className="h-4 w-4 rounded border-line-strong text-primary-ink focus:ring-focus"
            />
            <span>Đã kiểm tra nhận xét và điểm</span>
          </label>
        )}
      </div>
    </details>
  );
}
