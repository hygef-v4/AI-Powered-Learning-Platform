"use client";
import type { Checks, ItemScores, Rubric, StructuredFeedbackItem } from "../model/types";

import { resolveItemPoints, calculateChecklistPoints } from "../model/rubricScores";
import { RubricItemFeedback } from "./StructuredFeedbackSection";
import { msg } from "@/i18n/msg";

export type ChecklistProps = {
  checks: Checks;
  onChange: (value: Checks) => void;
  disabled?: boolean;
  itemScores?: ItemScores;
  onScoresChange?: (value: ItemScores, changedRubricId?: string, changedItemId?: string) => void;
  onSetItemScore?: (rubricId: string, itemId: string, score: number) => void;
  feedbackItems?: StructuredFeedbackItem[];
  acknowledgedReviewItems?: Checks;
  scoreReviewItems?: ReadonlySet<string>;
  validBlockIds?: Set<string> | string[];
  onChangeStructuredItem?: (rubricId: string, itemId: string, patch: Partial<StructuredFeedbackItem>) => void;
  onToggleAcknowledge?: (key: string) => void;
  /** Khóa `rubricId:itemId` của mục AI đã điền và chưa bị sửa tay. */
  aiFilledItems?: Set<string>;
};

// Đánh dấu phần giá trị được điền từ đề xuất AI để người chấm nhận ra nguồn.
export function AiFilledBadge() {
  return (
    <span title={msg("grading.components.AiDraftBanner.filledByAi")}
      className="shrink-0 rounded bg-primary-soft px-1 text-[10px] font-semibold text-primary-ink">
      <span aria-hidden="true">✨ </span>{msg("grading.components.AiDraftBanner.aiLabel")}
    </span>
  );
}

/**
 * Checkbox edits award full/zero points; number input allows partial points (0..max).
 */
export function RubricChecklist({
  rubric,
  checks,
  onChange,
  disabled = false,
  itemScores = {},
  onScoresChange,
  onSetItemScore,
  feedbackItems,
  acknowledgedReviewItems,
  scoreReviewItems,
  validBlockIds,
  onChangeStructuredItem,
  onToggleAcknowledge,
  aiFilledItems,
}: ChecklistProps & { rubric: Rubric }) {
  // Đồng bộ mục checklist và điểm tối đa khi đánh dấu đạt hoặc bỏ đánh dấu.
  function awardItem(itemId: string, max: number, achieved: boolean) {
    const selected = new Set(checks[rubric.rubricId] ?? []);
    if (achieved) selected.add(itemId); else selected.delete(itemId);
    onChange({ ...checks, [rubric.rubricId]: [...selected] });
    onScoresChange?.({ ...itemScores, [rubric.rubricId]: { ...itemScores[rubric.rubricId], [itemId]: achieved ? max : 0 } },
      rubric.rubricId, itemId);
  }
  // Đọc điểm nhập, bỏ giá trị không phải số và gửi cập nhật cho nhánh chấm phù hợp.
  function handleScoreInput(itemId: string, max: number, rawValue: string) {
    const parsed = rawValue === "" ? 0 : Number(rawValue);
    if (!Number.isFinite(parsed)) return;
    if (onSetItemScore) {
      onSetItemScore(rubric.rubricId, itemId, parsed);
      return;
    }
    const achieved = max > 0 && Math.round(parsed * 100) >= Math.round(max * 100);
    const selected = new Set(checks[rubric.rubricId] ?? []);
    if (achieved) selected.add(itemId); else selected.delete(itemId);
    onChange({ ...checks, [rubric.rubricId]: [...selected] });
    onScoresChange?.({ ...itemScores, [rubric.rubricId]: { ...itemScores[rubric.rubricId], [itemId]: parsed } },
      rubric.rubricId, itemId);
  }
  const items = rubric.criteria.flatMap((criterion) => criterion.items.map((item) => ({ criterion, item })));
  if (items.length === 0) return <p className="text-xs text-muted">{msg("grading.components.RubricChecklistPanel.thisPartHasNoCriteria")}</p>;
  return (
    <ul className="flex flex-col gap-1.5">
      {items.map(({ criterion, item }) => {
        const max = Number(item.points);
        const value = Number(resolveItemPoints(rubric.rubricId, item.itemId, max, checks, itemScores));
        const achieved = value === max;
        const showCriterion = criterion.title.trim() !== "" && criterion.title.trim() !== item.text.trim();
        const feedback = feedbackItems?.find(
          (entry) => entry.rubricId === rubric.rubricId && entry.itemId === item.itemId,
        );
        const ackKey = `${rubric.rubricId}:${item.itemId}`;
        const isAck = Boolean(
          acknowledgedReviewItems?.[rubric.rubricId]?.includes(item.itemId) ||
            acknowledgedReviewItems?.[ackKey]?.includes(item.itemId),
        );
        return (
          <li key={item.itemId} data-testid={`rubric-item-${item.itemId}`}
            className={`rounded-lg border px-2 py-1.5 text-xs leading-snug ${achieved ? "border-primary/30 bg-primary/5" : "border-line bg-surface"}`}>
            <div className="flex items-start justify-between gap-2">
              <label className="flex min-w-0 flex-1 cursor-pointer items-start gap-2">
                <input type="checkbox" aria-label={item.text} checked={achieved} disabled={disabled}
                  onChange={(event) => awardItem(item.itemId, max, event.target.checked)}
                  className="mt-0.5 size-4 shrink-0 accent-primary disabled:cursor-not-allowed" />
                <span className="min-w-0 flex-1 break-words text-ink">
                  {showCriterion && <span className="block text-xs text-faint">{criterion.title}</span>}
                  {item.text}
                </span>
              </label>
              <div className="flex shrink-0 items-center gap-1.5 tabular-nums text-muted">
                {aiFilledItems?.has(ackKey) && <AiFilledBadge />}
                {!disabled && (
                  <input
                    type="number"
                    min={0}
                    max={max}
                    step="0.1"
                    aria-label={msg("grading.components.RubricChecklistPanel.pointsForItem", { text: String(item.text) })}
                    value={Number.isFinite(value) ? value : 0}
                    onChange={(event) => handleScoreInput(item.itemId, max, event.target.value)}
                    className="w-16 rounded border border-line bg-surface px-1.5 py-0.5 text-right text-xs text-ink focus:border-focus focus:outline-none"
                  />
                )}
                <span>
                  {disabled && (feedback || (value > 0 && value < max)) ? `${value.toFixed(2)} / ` : !disabled ? "/ " : ""}{max.toFixed(2)}
                </span>
              </div>
            </div>
              <RubricItemFeedback
                rubricId={rubric.rubricId}
                itemId={item.itemId}
                itemText={item.text}
                awardedPoints={value}
                maxPoints={max}
                feedbackItem={feedback}
                acknowledged={isAck}
                scoreReviewRequired={scoreReviewItems?.has(ackKey)}
                editable={!disabled}
                disabled={disabled}
                validBlockIds={validBlockIds}
                fallbackBlockId={rubric.partId}
                onChangeItem={
                  onChangeStructuredItem
                    ? (patch) => onChangeStructuredItem(rubric.rubricId, item.itemId, patch)
                    : undefined
                }
                onToggleAcknowledge={
                  onToggleAcknowledge ? () => onToggleAcknowledge(ackKey) : undefined
                }
              />
          </li>
        );
      })}
    </ul>
  );
}

/** Checklist của mọi phần xếp dọc (dùng khi không có tài liệu để gắn ghi chú). */
export function RubricChecklistPanel({ rubrics, partTitles = {}, ...props }: ChecklistProps & {
  rubrics: Rubric[];
  partTitles?: Record<string, string>;
}) {
  return <section aria-label={msg("grading.components.RubricChecklistPanel.gradeWithTheRubric")}>
    <h2 className="text-base font-bold text-ink">{msg("grading.components.RubricChecklistPanel.gradeWithTheRubric")}</h2>
    <p className="mt-0.5 text-xs text-muted">{msg("grading.components.RubricChecklistPanel.tickTheItemsMetEach")}</p>
    {!rubrics.length && <p className="mt-3 rounded-lg bg-shell p-3 text-sm text-muted">{msg("grading.components.RubricChecklistPanel.theAssignmentHasNoUsable")}</p>}
    {rubrics.map((rubric, index) => {
      const title = partTitles[rubric.partId] || msg("grading.components.RubricChecklistPanel.part", { value: String(index + 1) });
      return <fieldset key={rubric.rubricId} disabled={props.disabled} aria-label={title}
        className="mt-3 min-w-0 rounded-xl border border-line bg-surface p-2.5 sm:p-3">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
          <span className="min-w-0 flex-1 break-words text-sm font-semibold text-ink">{title}</span>
          <span className="shrink-0 text-sm font-bold tabular-nums text-primary-ink">
            {calculateChecklistPoints([rubric], props.checks, props.itemScores).toFixed(2)} / {Number(rubric.maxPoints).toFixed(2)} {msg("grading.components.RubricChecklistPanel.points")}</span>
        </div>
        <RubricChecklist rubric={rubric} {...props} />
      </fieldset>;
    })}
  </section>;
}
