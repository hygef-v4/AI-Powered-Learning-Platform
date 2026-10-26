"use client";
import type { ReactNode } from "react";
import { RubricChecklistPanel } from "./RubricChecklistPanel";
import {
  GradingFeedbackSummary,
  countUnresolvedReviews,
} from "./StructuredFeedbackSection";
import type {
  GradingFormValues,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
} from "./types";

type Props = {
  rubrics: Rubric[];
  values: GradingFormValues;
  disabled: boolean;
  official: boolean;
  onChange: (patch: Partial<GradingFormValues>) => void;
  children?: ReactNode;
  partTitles?: Record<string, string>;
  validBlockIds?: Set<string>;
};

function ensureStructuredFeedback(current?: StructuredFeedback | null): StructuredFeedback {
  return (
    current ?? {
      schemaVersion: 1,
      summary: { strengths: [], improvements: [], nextSteps: [] },
      items: [],
    }
  );
}

export function GradingForm({
  rubrics,
  values,
  disabled,
  official,
  onChange,
  children,
  partTitles,
  validBlockIds,
}: Props) {
  const unresolved = countUnresolvedReviews(
    rubrics,
    values.checks,
    values.itemScores,
    values.structuredFeedback,
    values.reviewedItemScores,
    values.acknowledgedReviewItems,
  );

  function handleScoresChange(
    nextScores: GradingFormValues["itemScores"],
    changedRubricId?: string,
    changedItemId?: string,
  ) {
    if (!changedRubricId || !changedItemId || !values.acknowledgedReviewItems?.[changedRubricId]) {
      onChange({ itemScores: nextScores });
      return;
    }
    const nextAckRubric = (values.acknowledgedReviewItems[changedRubricId] ?? []).filter(
      (id) => id !== changedItemId,
    );
    onChange({
      itemScores: nextScores,
      acknowledgedReviewItems: {
        ...values.acknowledgedReviewItems,
        [changedRubricId]: nextAckRubric,
      },
    });
  }

  function handleItemFeedbackChange(updated: StructuredFeedbackItem, currentAwardedPoints: number) {
    const base = ensureStructuredFeedback(values.structuredFeedback);
    const exists = base.items.some(
      (entry) => entry.rubricId === updated.rubricId && entry.itemId === updated.itemId,
    );
    const nextItems = exists
      ? base.items.map((entry) =>
          entry.rubricId === updated.rubricId && entry.itemId === updated.itemId ? updated : entry,
        )
      : [...base.items, updated];
    const nextReviewedScores = {
      ...(values.reviewedItemScores ?? {}),
      [updated.rubricId]: {
        ...(values.reviewedItemScores?.[updated.rubricId] ?? {}),
        [updated.itemId]: Number.isFinite(currentAwardedPoints) ? currentAwardedPoints.toFixed(2) : "0.00",
      },
    };
    const existingAck = new Set(values.acknowledgedReviewItems?.[updated.rubricId] ?? []);
    if (updated.requiresReview) {
      existingAck.add(updated.itemId);
    }
    onChange({
      structuredFeedback: { ...base, items: nextItems },
      clearStructuredFeedback: false,
      reviewedItemScores: nextReviewedScores,
      acknowledgedReviewItems: {
        ...(values.acknowledgedReviewItems ?? {}),
        [updated.rubricId]: [...existingAck],
      },
    });
  }

  function handleToggleAcknowledge(
    rubricId: string,
    itemId: string,
    acknowledged: boolean,
    currentAwardedPoints: number,
  ) {
    const existingAck = new Set(values.acknowledgedReviewItems?.[rubricId] ?? []);
    if (acknowledged) {
      existingAck.add(itemId);
    } else {
      existingAck.delete(itemId);
    }
    const nextReviewedScores = acknowledged
      ? {
          ...(values.reviewedItemScores ?? {}),
          [rubricId]: {
            ...(values.reviewedItemScores?.[rubricId] ?? {}),
            [itemId]: Number.isFinite(currentAwardedPoints) ? currentAwardedPoints.toFixed(2) : "0.00",
          },
        }
      : values.reviewedItemScores;
    onChange({
      acknowledgedReviewItems: {
        ...(values.acknowledgedReviewItems ?? {}),
        [rubricId]: [...existingAck],
      },
      reviewedItemScores: nextReviewedScores,
    });
  }

  return (
    <div className="min-w-0 rounded-xl border border-line bg-white p-3.5 sm:p-4">
      {unresolved.total > 0 && (
        <div
          role="status"
          className="mb-3 rounded-lg border border-warning bg-warning-soft/60 px-3 py-2 text-xs text-ink"
        >
          <p className="font-semibold text-warning">
            ⚠ Còn {unresolved.total} mục nhận xét cấu trúc cần giảng viên kiểm tra hoặc xác nhận lại trước khi chốt điểm.
          </p>
        </div>
      )}
      <RubricChecklistPanel
        partTitles={partTitles}
        rubrics={rubrics}
        checks={values.checks}
        itemScores={values.itemScores}
        onScoresChange={handleScoresChange}
        disabled={disabled}
        onChange={(value) => onChange({ checks: value })}
        structuredFeedback={values.structuredFeedback}
        reviewedItemScores={values.reviewedItemScores}
        acknowledgedReviewItems={values.acknowledgedReviewItems}
        validBlockIds={validBlockIds}
        onItemFeedbackChange={handleItemFeedbackChange}
        onToggleAcknowledge={handleToggleAcknowledge}
      />
      <div className="mt-3 flex flex-wrap items-center gap-3 text-sm">
        <label className="flex items-center gap-2 font-medium text-ink">
          Điều chỉnh (+/−)
          <input
            type="number"
            step="0.01"
            value={values.adjustment}
            disabled={disabled}
            onChange={(v) => onChange({ adjustment: v.target.value })}
            className="w-24 rounded-lg border border-line px-2 py-1.5 tabular-nums focus:border-primary focus:outline-none"
          />
        </label>
      </div>
      <label className="mt-2.5 block text-sm font-medium text-ink">
        Lý do điều chỉnh {Number(values.adjustment) !== 0 && "(bắt buộc)"}
        <input
          className="mt-1 block w-full rounded-lg border border-line px-2.5 py-1.5 text-sm font-normal focus:border-primary focus:outline-none"
          value={values.adjustmentReason}
          maxLength={2000}
          disabled={disabled}
          onChange={(v) => onChange({ adjustmentReason: v.target.value })}
        />
      </label>

      {values.structuredFeedback ? (
        <GradingFeedbackSummary
          summary={values.structuredFeedback.summary}
          title="Tóm tắt nhận xét cấu trúc (Làm tốt · Cần sửa · Gợi ý tiếp theo)"
          editable
          disabled={disabled}
          onChange={(nextSummary) =>
            onChange({
              structuredFeedback: {
                ...ensureStructuredFeedback(values.structuredFeedback),
                summary: nextSummary,
              },
              clearStructuredFeedback: false,
            })
          }
          onClear={() =>
            onChange({
              structuredFeedback: null,
              clearStructuredFeedback: true,
              acknowledgedReviewItems: {},
              reviewedItemScores: {},
            })
          }
        />
      ) : (
        <div className="mt-2.5">
          <button
            type="button"
            disabled={disabled}
            onClick={() =>
              onChange({
                structuredFeedback: ensureStructuredFeedback(null),
                clearStructuredFeedback: false,
              })
            }
            className="rounded-lg border border-line-strong bg-shell/60 px-2.5 py-1.5 text-xs font-medium text-primary hover:bg-shell disabled:opacity-50"
          >
            + Thêm tóm tắt nhận xét cấu trúc (Làm tốt / Cần sửa / Gợi ý tiếp theo)
          </button>
        </div>
      )}

      <label className="mt-2.5 block text-sm font-medium text-ink">
        Nhận xét
        <textarea
          className="mt-1 block min-h-24 w-full rounded-lg border border-line p-2.5 text-sm font-normal focus:border-primary focus:outline-none"
          value={values.feedback}
          maxLength={10000}
          disabled={disabled}
          onChange={(v) => onChange({ feedback: v.target.value })}
        />
      </label>
      {official && (
        <label className="mt-2.5 block text-sm font-medium text-ink">
          Lý do sửa điểm đã chốt/công bố (bắt buộc)
          <input
            className="mt-1 block w-full rounded-lg border border-line px-2.5 py-1.5 text-sm font-normal focus:border-primary focus:outline-none"
            value={values.changeReason}
            maxLength={2000}
            onChange={(v) => onChange({ changeReason: v.target.value })}
          />
        </label>
      )}
      {children}
    </div>
  );
}
