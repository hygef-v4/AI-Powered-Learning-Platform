"use client";
import { useEffect, useRef, useState } from "react";
import { RubricItemFeedback, isItemAcknowledged } from "./StructuredFeedbackSection";
import type {
  Checks,
  ItemScores,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
} from "./types";

export function itemPoints(rubricId: string, itemId: string, max: number, checks: Checks, scores: ItemScores = {}) {
  return scores[rubricId]?.[itemId] ?? ((checks[rubricId] ?? []).includes(itemId) ? max : 0);
}
export function validItemScore(value: string | number, maximum: number) {
  return /^\d+(?:\.\d{1,2})?$/.test(String(value)) && Number(value) >= 0 && Number(value) <= maximum;
}
export function invalidItemScores(rubrics: Rubric[], scores: ItemScores = {}) {
  return rubrics.some((rubric) => rubric.criteria.some((criterion) => criterion.items.some((item) => {
    const value = scores[rubric.rubricId]?.[item.itemId];
    return value !== undefined && !validItemScore(value, Number(item.points));
  })));
}
export function hasPartialPoints(rubrics: Rubric[], checks: Checks, scores: ItemScores = {}) {
  return rubrics.some((rubric) => rubric.criteria.some((criterion) => criterion.items.some((item) => {
    const value = Number(itemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, scores));
    return value > 0 && value < Number(item.points);
  })));
}
/** Same decimal-cent sum as the backend; item overrides take precedence over legacy checks. */
export function checklistPoints(rubrics: Rubric[], checks: Checks, scores: ItemScores = {}): number {
  return rubrics.reduce((total, rubric) => total + rubric.criteria.reduce((sum, criterion) =>
    sum + criterion.items.reduce((points, item) => {
      const value = Number(itemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, scores));
      return points + (Number.isFinite(value) ? Math.round(value * 100) : 0);
    }, 0), 0), 0) / 100;
}

export function RubricChecklistPanel({
  rubrics,
  checks,
  onChange,
  disabled = false,
  partTitles = {},
  itemScores = {},
  onScoresChange,
  structuredFeedback,
  reviewedItemScores = {},
  acknowledgedReviewItems = {},
  validBlockIds,
  onItemFeedbackChange,
  onToggleAcknowledge,
}: {
  rubrics: Rubric[];
  checks: Checks;
  onChange: (value: Checks) => void;
  disabled?: boolean;
  partTitles?: Record<string, string>;
  itemScores?: ItemScores;
  onScoresChange?: (value: ItemScores, changedRubricId?: string, changedItemId?: string) => void;
  structuredFeedback?: StructuredFeedback | null;
  reviewedItemScores?: ItemScores;
  acknowledgedReviewItems?: Checks;
  validBlockIds?: Set<string>;
  onItemFeedbackChange?: (item: StructuredFeedbackItem, currentAwardedPoints: number) => void;
  onToggleAcknowledge?: (rubricId: string, itemId: string, acknowledged: boolean, currentAwardedPoints: number) => void;
}) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const pendingFocusRef = useRef<string | null>(null);

  useEffect(() => {
    if (pendingFocusRef.current) {
      document.getElementById(pendingFocusRef.current)?.focus();
      pendingFocusRef.current = null;
    }
  }, [collapsed]);

  function award(rubricId: string, itemId: string, max: number, value: string | number) {
    const selected = new Set(checks[rubricId] ?? []);
    if (Number(value) === max && validItemScore(value, max)) selected.add(itemId); else selected.delete(itemId);
    onChange({ ...checks, [rubricId]: [...selected] });
    onScoresChange?.({ ...itemScores, [rubricId]: { ...itemScores[rubricId], [itemId]: value } }, rubricId, itemId);
  }
  return <section aria-label="Chấm theo rubric">
    <h2 className="text-base font-bold text-ink">Chấm theo rubric</h2>
    <p className="mt-0.5 text-xs text-muted">Nhập điểm từng mục hoặc bấm Đủ điểm. Hỗ trợ điểm lẻ (ví dụ 0.5 / 1, tối đa 2 chữ số thập phân).</p>
    {!rubrics.length && <p className="mt-3 rounded-lg bg-shell p-3 text-sm text-muted">Bài chưa có rubric khả dụng. Hãy kiểm tra lại cấu hình bài tập.</p>}
    {rubrics.map((rubric, index) => {
      const title = partTitles[rubric.partId] || `Phần ${index + 1}`;
      const isCollapsed = !!collapsed[rubric.rubricId];
      const firstInvalidItem = rubric.criteria.flatMap((c) => c.items).find((item) => {
        const val = itemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, itemScores);
        return !validItemScore(val, Number(item.points));
      });
      return <fieldset key={rubric.rubricId} disabled={disabled} aria-label={title}
        className="mt-3 min-w-0 rounded-xl border border-line bg-white p-2.5 sm:p-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <button
            type="button"
            aria-expanded={!isCollapsed}
            onClick={() => setCollapsed((prev) => ({ ...prev, [rubric.rubricId]: !prev[rubric.rubricId] }))}
            className="inline-flex min-w-0 flex-1 items-center gap-1.5 text-left text-sm font-semibold text-ink hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            <span aria-hidden="true" className="text-xs text-muted">{isCollapsed ? "▸" : "▾"}</span>
            <span className="break-words">{title}</span>
          </button>
          <span className="shrink-0 text-sm font-bold tabular-nums text-primary">
            {checklistPoints([rubric], checks, itemScores).toFixed(2)} / {Number(rubric.maxPoints).toFixed(2)} điểm
          </span>
        </div>
        {firstInvalidItem && isCollapsed && (
          <div className="mt-2 flex items-center justify-between gap-2 rounded-lg border border-danger-line bg-danger-soft px-2.5 py-1.5 text-xs text-danger">
            <span>Phần này có mục điểm chưa hợp lệ.</span>
            <button
              type="button"
              className="font-semibold underline underline-offset-2 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-danger"
              onClick={() => {
                pendingFocusRef.current = `score-input-${rubric.rubricId}-${firstInvalidItem.itemId}`;
                setCollapsed((prev) => ({ ...prev, [rubric.rubricId]: false }));
              }}
            >
              Mở mục có điểm chưa hợp lệ
            </button>
          </div>
        )}
        <div hidden={isCollapsed} className="mt-2 space-y-2">
          {rubric.criteria.map((criterion) => <div key={criterion.criterionId} className="space-y-1.5">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-body">{criterion.title}</h3>
            {criterion.items.map((item) => {
              const max = Number(item.points);
              const value = itemPoints(rubric.rubricId, item.itemId, max, checks, itemScores);
              const valid = validItemScore(value, max);
              const full = Number(value) === max && valid;
              const inputId = `score-input-${rubric.rubricId}-${item.itemId}`;
              const errorId = `score-error-${rubric.rubricId}-${item.itemId}`;
              const fbItem = structuredFeedback?.items?.find(
                (entry) => entry.rubricId === rubric.rubricId && entry.itemId === item.itemId,
              );
              const revScore = reviewedItemScores?.[rubric.rubricId]?.[item.itemId];
              const ack = isItemAcknowledged(acknowledgedReviewItems, rubric.rubricId, item.itemId);
              return <div key={item.itemId} className={`rounded-lg border px-2.5 py-2 text-sm leading-snug focus-within:ring-2 focus-within:ring-primary/30 ${!valid ? "border-danger-line bg-danger-soft/40" : Number(value) > 0 ? "border-primary/30 bg-primary/5" : "border-line bg-shell/60"}`}>
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <label htmlFor={inputId} className="min-w-0 flex-1 cursor-pointer break-words text-ink">
                    {item.text}
                  </label>
                  <div className="flex shrink-0 items-center gap-1">
                    <input id={inputId} type="number" inputMode="decimal" aria-label={`Điểm: ${item.text}`} min={0} max={max} step="0.01"
                      disabled={disabled}
                      value={value} aria-invalid={!valid} aria-describedby={!valid ? errorId : undefined}
                      className="w-16 rounded-md border border-line-strong bg-white px-1.5 py-1 text-right font-semibold tabular-nums text-ink focus:border-primary focus:outline-none disabled:opacity-60"
                      onChange={(event) => award(rubric.rubricId, item.itemId, max, event.target.value)} />
                    <span className="text-xs whitespace-nowrap tabular-nums text-muted">/ {max.toFixed(2)}</span>
                    <button
                      type="button"
                      disabled={disabled}
                      aria-pressed={full}
                      aria-label={`Đủ điểm: ${item.text}`}
                      onClick={() => award(rubric.rubricId, item.itemId, max, max)}
                      className={`rounded-md border px-2 py-1 text-xs font-semibold whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50 ${
                        full
                          ? "border-primary bg-primary text-white"
                          : "border-line-strong bg-white text-body hover:bg-shell"
                      }`}
                    >
                      {full ? "✓ Đủ điểm" : "Đủ điểm"}
                    </button>
                  </div>
                </div>
                {!valid && <p id={errorId} role="alert" className="mt-1.5 text-xs text-danger">Nhập từ 0 đến {max} điểm, tối đa 2 chữ số thập phân.</p>}
                {(fbItem || onItemFeedbackChange) && (
                  <RubricItemFeedback
                    rubricId={rubric.rubricId}
                    itemId={item.itemId}
                    itemText={item.text}
                    awardedPoints={valid ? Number(value) : NaN}
                    maxPoints={max}
                    feedbackItem={fbItem}
                    reviewedScore={revScore}
                    acknowledged={ack}
                    validBlockIds={validBlockIds}
                    editable={Boolean(onItemFeedbackChange)}
                    disabled={disabled}
                    onChangeItem={
                      onItemFeedbackChange
                        ? (updated) => onItemFeedbackChange(updated, valid ? Number(value) : 0)
                        : undefined
                    }
                    onToggleAcknowledge={
                      onToggleAcknowledge
                        ? (checked) => onToggleAcknowledge(rubric.rubricId, item.itemId, checked, valid ? Number(value) : 0)
                        : undefined
                    }
                  />
                )}
              </div>;
            })}
          </div>)}
        </div>
      </fieldset>;
    })}
  </section>;
}
