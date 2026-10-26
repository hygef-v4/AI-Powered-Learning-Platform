"use client";
import {
  GradingFeedbackSummary,
  countUnresolvedReviews,
} from "./StructuredFeedbackSection";
import type { ChecklistProps } from "./RubricChecklistPanel";
import type {
  GradingFormValues,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
} from "./types";

type OnChange = (patch: Partial<GradingFormValues>) => void;

function ensureStructuredFeedback(current?: StructuredFeedback | null): StructuredFeedback {
  return (
    current ?? {
      schemaVersion: 1,
      summary: { strengths: [], improvements: [], nextSteps: [] },
      items: [],
    }
  );
}

/** Nối checklist của mọi phần với biểu mẫu chấm: tích mục, nhận xét từng mục, xác nhận mục AI cần kiểm tra. */
export function checklistBindings(values: GradingFormValues, disabled: boolean, onChange: OnChange): ChecklistProps {
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

  return {
    checks: values.checks,
    onChange: (checks) => onChange({ checks }),
    disabled,
    itemScores: values.itemScores,
    onScoresChange: handleScoresChange,
    structuredFeedback: values.structuredFeedback,
    reviewedItemScores: values.reviewedItemScores,
    acknowledgedReviewItems: values.acknowledgedReviewItems,
    onItemFeedbackChange: handleItemFeedbackChange,
    onToggleAcknowledge: handleToggleAcknowledge,
  };
}

/**
 * Ghi chú "Nhận xét chung" ở đầu bài nộp: nhắc mục AI cần kiểm tra, tóm tắt Làm tốt · Cần sửa, nhận xét gửi sinh viên
 * và lý do khi sửa điểm đã chốt. Điểm bài là tổng mục đạt, không có điều chỉnh ngoài rubric (BR-U09-20).
 */
export function GradingNotes({
  rubrics,
  values,
  disabled,
  official,
  onChange,
}: {
  rubrics: Rubric[];
  values: GradingFormValues;
  disabled: boolean;
  official: boolean;
  onChange: OnChange;
}) {
  const unresolved = countUnresolvedReviews(
    rubrics,
    values.checks,
    values.itemScores,
    values.structuredFeedback,
    values.reviewedItemScores,
    values.acknowledgedReviewItems,
  );

  return (
    <section aria-label="Nhận xét chung" data-testid="grading-notes"
      className="flex min-w-0 flex-col gap-2 rounded-xl border border-line bg-surface p-3 text-xs shadow-sm">
      <p className="text-[13px] font-semibold text-ink">Nhận xét chung</p>
      {unresolved.total > 0 && (
        <p role="status" className="rounded-lg border border-warning bg-warning-soft/60 px-2.5 py-1.5 font-semibold text-warning">
          ⚠ Còn {unresolved.total} mục nhận xét cần kiểm tra hoặc xác nhận lại trước khi chốt điểm.
        </p>
      )}
      <label className="block font-medium text-ink">
        Nhận xét gửi sinh viên
        <textarea
          className="mt-1 block min-h-20 w-full rounded-lg border border-line p-2 text-xs font-normal focus:border-focus focus:outline-none"
          value={values.feedback}
          maxLength={10000}
          disabled={disabled}
          onChange={(v) => onChange({ feedback: v.target.value })}
        />
      </label>
      {values.structuredFeedback ? (
        <GradingFeedbackSummary
          summary={values.structuredFeedback.summary}
          title="Làm tốt · Cần sửa"
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
        <button
          type="button"
          disabled={disabled}
          onClick={() =>
            onChange({
              structuredFeedback: ensureStructuredFeedback(null),
              clearStructuredFeedback: false,
            })
          }
          className="self-start rounded-lg border border-line-strong bg-shell/60 px-2.5 py-1 font-medium text-primary-ink hover:bg-shell disabled:opacity-50"
        >
          + Thêm tóm tắt Làm tốt · Cần sửa
        </button>
      )}
      {official && (
        <label className="block font-medium text-ink">
          Lý do sửa điểm đã chốt/công bố (bắt buộc)
          <input
            className="mt-1 block w-full rounded-lg border border-line px-2 py-1.5 text-xs font-normal focus:border-focus focus:outline-none"
            value={values.changeReason}
            maxLength={2000}
            onChange={(v) => onChange({ changeReason: v.target.value })}
          />
        </label>
      )}
    </section>
  );
}
