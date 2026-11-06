"use client";
import { RubricItemFeedback, isItemAcknowledged, isScoreChangedAfterReview } from "./StructuredFeedbackSection";
import type {
  Checks,
  ItemScores,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
} from "../model/types";

import { resolveItemPoints, calculateChecklistPoints } from "../model/rubricScores";

export type ChecklistProps = {
  checks: Checks;
  onChange: (value: Checks) => void;
  disabled?: boolean;
  itemScores?: ItemScores;
  onScoresChange?: (value: ItemScores, changedRubricId?: string, changedItemId?: string) => void;
  structuredFeedback?: StructuredFeedback | null;
  reviewedItemScores?: ItemScores;
  acknowledgedReviewItems?: Checks;
  validBlockIds?: Set<string>;
  onItemFeedbackChange?: (item: StructuredFeedbackItem, currentAwardedPoints: number) => void;
  onToggleAcknowledge?: (rubricId: string, itemId: string, acknowledged: boolean, currentAwardedPoints: number) => void;
};

/**
 * Checkbox edits award full/zero points; previously stored or proposed partial values remain visible until edited.
 * Review controls appear for flagged explanations or scores changed since their last review.
 */
export function RubricChecklist({
  rubric,
  checks,
  onChange,
  disabled = false,
  itemScores = {},
  onScoresChange,
  structuredFeedback,
  reviewedItemScores = {},
  acknowledgedReviewItems = {},
  validBlockIds,
  onItemFeedbackChange,
  onToggleAcknowledge,
}: ChecklistProps & { rubric: Rubric }) {
  function award(itemId: string, max: number, achieved: boolean) {
    const selected = new Set(checks[rubric.rubricId] ?? []);
    if (achieved) selected.add(itemId); else selected.delete(itemId);
    onChange({ ...checks, [rubric.rubricId]: [...selected] });
    onScoresChange?.({ ...itemScores, [rubric.rubricId]: { ...itemScores[rubric.rubricId], [itemId]: achieved ? max : 0 } },
      rubric.rubricId, itemId);
  }
  const items = rubric.criteria.flatMap((criterion) => criterion.items.map((item) => ({ criterion, item })));
  if (items.length === 0) return <p className="text-xs text-muted">Phần này chưa có mục chấm.</p>;
  return (
    <ul className="flex flex-col gap-1.5">
      {items.map(({ criterion, item }) => {
        const max = Number(item.points);
        const value = Number(resolveItemPoints(rubric.rubricId, item.itemId, max, checks, itemScores));
        const achieved = value === max;
        const fbItem = structuredFeedback?.items?.find(
          (entry) => entry.rubricId === rubric.rubricId && entry.itemId === item.itemId,
        );
        const showCriterion = criterion.title.trim() !== "" && criterion.title.trim() !== item.text.trim();
        const review = Boolean(fbItem?.requiresReview || isScoreChangedAfterReview(fbItem,
          reviewedItemScores[rubric.rubricId]?.[item.itemId], value,
          isItemAcknowledged(acknowledgedReviewItems, rubric.rubricId, item.itemId)));
        return (
          <li key={item.itemId} data-testid={`rubric-item-${item.itemId}`}
            className={`rounded-lg border px-2 py-1.5 text-xs leading-snug ${achieved ? "border-primary/30 bg-primary/5" : "border-line bg-surface"}`}>
            <label className="flex cursor-pointer items-start gap-2">
              <input type="checkbox" aria-label={item.text} checked={achieved} disabled={disabled}
                onChange={(event) => award(item.itemId, max, event.target.checked)}
                className="mt-0.5 size-4 shrink-0 accent-primary disabled:cursor-not-allowed" />
              <span className="min-w-0 flex-1 break-words text-ink">
                {showCriterion && <span className="block text-[10.5px] text-faint">{criterion.title}</span>}
                {item.text}
              </span>
              <span className="shrink-0 tabular-nums text-muted">{value > 0 && value < max ? `${value.toFixed(2)} / ` : ""}{max.toFixed(2)}</span>
            </label>
            {review && (
              <div className="pl-6">
                <RubricItemFeedback
                  rubricId={rubric.rubricId}
                  itemId={item.itemId}
                  itemText={item.text}
                  awardedPoints={value}
                  maxPoints={max}
                  feedbackItem={fbItem}
                  reviewedScore={reviewedItemScores[rubric.rubricId]?.[item.itemId]}
                  acknowledged={isItemAcknowledged(acknowledgedReviewItems, rubric.rubricId, item.itemId)}
                  validBlockIds={validBlockIds}
                  editable={Boolean(onItemFeedbackChange)}
                  disabled={disabled}
                  onChangeItem={onItemFeedbackChange ? (updated) => onItemFeedbackChange(updated, value) : undefined}
                  onToggleAcknowledge={onToggleAcknowledge
                    ? (checked) => onToggleAcknowledge(rubric.rubricId, item.itemId, checked, value)
                    : undefined}
                />
              </div>
            )}
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
  return <section aria-label="Chấm theo rubric">
    <h2 className="text-base font-bold text-ink">Chấm theo rubric</h2>
    <p className="mt-0.5 text-xs text-muted">Tích mục đạt; mỗi mục đạt được đủ điểm của mục.</p>
    {!rubrics.length && <p className="mt-3 rounded-lg bg-shell p-3 text-sm text-muted">Bài chưa có rubric khả dụng. Hãy kiểm tra lại cấu hình bài tập.</p>}
    {rubrics.map((rubric, index) => {
      const title = partTitles[rubric.partId] || `Phần ${index + 1}`;
      return <fieldset key={rubric.rubricId} disabled={props.disabled} aria-label={title}
        className="mt-3 min-w-0 rounded-xl border border-line bg-surface p-2.5 sm:p-3">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
          <span className="min-w-0 flex-1 break-words text-sm font-semibold text-ink">{title}</span>
          <span className="shrink-0 text-sm font-bold tabular-nums text-primary-ink">
            {calculateChecklistPoints([rubric], props.checks, props.itemScores).toFixed(2)} / {Number(rubric.maxPoints).toFixed(2)} điểm
          </span>
        </div>
        <RubricChecklist rubric={rubric} {...props} />
      </fieldset>;
    })}
  </section>;
}
