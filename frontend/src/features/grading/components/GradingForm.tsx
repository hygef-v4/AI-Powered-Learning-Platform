"use client";
import type { ChecklistProps } from "./RubricChecklistPanel";
import type { GradingFormValues } from "../model/types";

type OnChange = (patch: Partial<GradingFormValues>) => void;

/** Nối checklist với biểu mẫu chấm điểm. */
export function buildChecklistBindings(values: GradingFormValues, disabled: boolean, onChange: OnChange): ChecklistProps {
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

  return {
    checks: values.checks,
    onChange: (checks) => onChange({ checks }),
    disabled,
    itemScores: values.itemScores,
    onScoresChange: handleScoresChange,
  };
}
