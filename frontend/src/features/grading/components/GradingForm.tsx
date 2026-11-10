"use client";
import { buildRubricValues, resolveItemPoints } from "../model/rubricScores";
import type { ChecklistProps } from "./RubricChecklistPanel";
import type {
  Checks,
  Evaluation,
  GradingFormValues,
  ItemScores,
  Rubric,
  StructuredFeedback,
  StructuredFeedbackItem,
  StructuredFeedbackSummary,
} from "../model/types";

type OnChange = (patch: Partial<GradingFormValues>) => void;

/** Opening a grade preserves explicit item scores and historical adjustments. */
export function createInitialForm(evaluation: Evaluation, rubrics: Rubric[]): GradingFormValues {
  let checks: Checks;
  try {
    checks = JSON.parse(evaluation.rubricChecks);
  } catch {
    checks = {};
  }
  const normalized = buildRubricValues(rubrics, checks, evaluation.rubricItemScores ?? {});
  return {
    checks: normalized.checks,
    itemScores: normalized.itemScores,
    structuredFeedback: evaluation.structuredFeedback ?? null,
    clearStructuredFeedback: false,
    acknowledgedReviewItems: evaluation.acknowledgedReviewItems ?? {},
    reviewedItemScores: evaluation.reviewedItemScores ?? {},
    adjustment: String(evaluation.adjustment ?? 0),
    feedback: evaluation.feedback ?? "",
    changeReason: evaluation.adjustmentReason ?? "",
    usedProposalId: null,
  };
}

function findItemMaxPoints(rubrics: Rubric[], rubricId: string, itemId: string): number {
  return Number(
    rubrics
      .find((r) => r.rubricId === rubricId)
      ?.criteria.flatMap((c) => c.items)
      .find((i) => i.itemId === itemId)?.points ?? 0,
  );
}

export function buildItemScorePatch(
  rubrics: Rubric[],
  form: GradingFormValues,
  rubricId: string,
  itemId: string,
  score: number,
): Partial<GradingFormValues> {
  const max = findItemMaxPoints(rubrics, rubricId, itemId);
  const achieved = Number.isFinite(score) && max > 0 && Math.round(score * 100) >= Math.round(max * 100);
  const selected = new Set(form.checks[rubricId] ?? []);
  if (achieved) selected.add(itemId);
  else selected.delete(itemId);
  const nextAck = (form.acknowledgedReviewItems?.[rubricId] ?? []).filter((id) => id !== itemId);
  return {
    checks: { ...form.checks, [rubricId]: [...selected] },
    itemScores: { ...form.itemScores, [rubricId]: { ...form.itemScores?.[rubricId], [itemId]: score } },
    ...(form.acknowledgedReviewItems?.[rubricId]
      ? { acknowledgedReviewItems: { ...form.acknowledgedReviewItems, [rubricId]: nextAck } }
      : {}),
  };
}

export function buildStructuredSummaryPatch(
  form: GradingFormValues,
  summary: StructuredFeedbackSummary,
): Partial<GradingFormValues> {
  const base = form.structuredFeedback ?? {
    schemaVersion: 1,
    summary: { strengths: [], improvements: [], nextSteps: [] },
    items: [],
  };
  return {
    structuredFeedback: { ...base, summary },
    clearStructuredFeedback: false,
  };
}

export function buildStructuredItemPatch(
  rubrics: Rubric[],
  form: GradingFormValues,
  rubricId: string,
  itemId: string,
  patch: Partial<StructuredFeedbackItem>,
): Partial<GradingFormValues> {
  const base = form.structuredFeedback ?? {
    schemaVersion: 1,
    summary: { strengths: [], improvements: [], nextSteps: [] },
    items: [],
  };
  const existing = base.items.find((entry) => entry.rubricId === rubricId && entry.itemId === itemId);
  const updated: StructuredFeedbackItem = existing
    ? { ...existing, ...patch, rubricId, itemId }
    : {
        rubricId,
        itemId,
        evidence: null,
        reason: "",
        suggestion: null,
        evidenceBlockId: null,
        requiresReview: false,
        reviewReason: null,
        ...patch,
      };
  const nextItems = existing
    ? base.items.map((entry) => (entry.rubricId === rubricId && entry.itemId === itemId ? updated : entry))
    : [...base.items, updated];
  const currentScore = Number(
    resolveItemPoints(rubricId, itemId, findItemMaxPoints(rubrics, rubricId, itemId), form.checks, form.itemScores),
  );
  const scoreStr = Number.isFinite(currentScore) ? currentScore.toFixed(2) : "0.00";
  const existingAck = new Set(form.acknowledgedReviewItems?.[rubricId] ?? []);
  if (updated.requiresReview) existingAck.add(itemId);
  return {
    structuredFeedback: { ...base, items: nextItems },
    clearStructuredFeedback: false,
    reviewedItemScores: {
      ...(form.reviewedItemScores ?? {}),
      [rubricId]: { ...(form.reviewedItemScores?.[rubricId] ?? {}), [itemId]: scoreStr },
    },
    acknowledgedReviewItems: { ...(form.acknowledgedReviewItems ?? {}), [rubricId]: [...existingAck] },
  };
}

export function buildToggleAcknowledgePatch(
  rubrics: Rubric[],
  form: GradingFormValues,
  key: string,
): Partial<GradingFormValues> | null {
  const [rubricId, itemId] = key.includes(":") ? (key.split(":", 2) as [string, string]) : [key, ""];
  if (!rubricId || !itemId) return null;
  const existingAck = new Set(form.acknowledgedReviewItems?.[rubricId] ?? []);
  const willAck = !existingAck.has(itemId);
  if (willAck) existingAck.add(itemId);
  else existingAck.delete(itemId);
  const currentScore = Number(
    resolveItemPoints(rubricId, itemId, findItemMaxPoints(rubrics, rubricId, itemId), form.checks, form.itemScores),
  );
  const scoreStr = Number.isFinite(currentScore) ? currentScore.toFixed(2) : "0.00";
  return {
    acknowledgedReviewItems: { ...(form.acknowledgedReviewItems ?? {}), [rubricId]: [...existingAck] },
    ...(willAck
      ? {
          reviewedItemScores: {
            ...(form.reviewedItemScores ?? {}),
            [rubricId]: { ...(form.reviewedItemScores?.[rubricId] ?? {}), [itemId]: scoreStr },
          },
        }
      : {}),
  };
}

export function buildProposalDraftPatch(
  rubrics: Rubric[],
  evaluation: Evaluation,
  form: GradingFormValues,
  checks: Checks,
  feedback: string,
  proposalId: string,
  itemScores: ItemScores = {},
  structuredFeedback?: StructuredFeedback | null,
): Partial<GradingFormValues> {
  const normalized = buildRubricValues(rubrics, checks, itemScores);
  const retainedFeedback = structuredFeedback ?? form.structuredFeedback ?? evaluation.structuredFeedback ?? null;
  const reviewedItemScores: ItemScores = {};
  if (!structuredFeedback && retainedFeedback) {
    for (const { rubricId, itemId } of retainedFeedback.items) {
      const score = form.reviewedItemScores?.[rubricId]?.[itemId] ?? form.itemScores?.[rubricId]?.[itemId];
      if (score !== undefined) (reviewedItemScores[rubricId] ??= {})[itemId] = score;
    }
  }
  return {
    checks: normalized.checks,
    itemScores: normalized.itemScores,
    feedback,
    structuredFeedback: retainedFeedback,
    clearStructuredFeedback: false,
    acknowledgedReviewItems: {},
    reviewedItemScores,
    usedProposalId: proposalId,
  };
}

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
