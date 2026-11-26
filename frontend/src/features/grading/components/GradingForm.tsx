"use client";
import { buildRubricValues, resolveItemPoints } from "../model/rubricScores";
import type { ChecklistProps } from "./RubricChecklistPanel";
import type {
  Checks,
  Evaluation,
  GradingFormValues,
  ItemScores,
  Proposal,
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

const REVIEW_FIELDS = ["structuredFeedback", "clearStructuredFeedback", "acknowledgedReviewItems",
  "reviewedItemScores", "usedProposalId"] as const;
const RUBRIC_FIELDS = ["checks", "itemScores", ...REVIEW_FIELDS] as const;
const DRAFT_FIELD_GROUPS = [
  ["feedback"],
  ["adjustment", "changeReason"],
  RUBRIC_FIELDS,
] as const satisfies readonly (readonly (keyof GradingFormValues)[])[];

// Tạo biểu diễn dùng so sánh một nhóm trường liên quan trong lúc hợp nhất.
function readDraftFields(form: GradingFormValues, keys: readonly (keyof GradingFormValues)[]) {
  return JSON.stringify(keys.map((key) => form[key]));
}

// Đưa chuỗi điểm hợp lệ về số để các cách viết tương đương so sánh bằng nhau.
function comparableScore(value: string | number | undefined) {
  return value !== undefined && /^\d+(?:\.\d+)?$/.test(String(value)) ? Number(value) : value;
}

// So bản gốc, bản đang sửa và bản server để giữ thay đổi cục bộ và phát hiện trùng trường.
export function rebaseGradingForm(
  base: GradingFormValues,
  draft: GradingFormValues,
  server: GradingFormValues,
  rubrics: Rubric[],
) {
  const form = { ...server };
  let dirty = false;
  let conflict = false;

  // Giữ giá trị đang sửa của một trường trên bản hợp nhất.
  function keepField<Key extends keyof GradingFormValues>(key: Key) {
    form[key] = draft[key];
  }

  for (const keys of DRAFT_FIELD_GROUPS) {
    const original = readDraftFields(base, keys);
    const local = readDraftFields(draft, keys);
    const remote = readDraftFields(server, keys);
    if (local === original || local === remote) continue;
    if (remote === original) {
      keys.forEach(keepField);
      dirty = true;
    } else if (keys === RUBRIC_FIELDS
        && readDraftFields(base, REVIEW_FIELDS) === readDraftFields(draft, REVIEW_FIELDS)
        && readDraftFields(base, REVIEW_FIELDS) === readDraftFields(server, REVIEW_FIELDS)) {
      const mergedScores: ItemScores = {};
      for (const [rubricId, scores] of Object.entries(server.itemScores ?? {})) {
        const merged = { ...scores };
        for (const [itemId, remoteScore] of Object.entries(scores)) {
          const originalScore = comparableScore(base.itemScores?.[rubricId]?.[itemId]);
          const localScore = draft.itemScores?.[rubricId]?.[itemId];
          if (localScore === undefined || comparableScore(localScore) === originalScore
              || comparableScore(localScore) === comparableScore(remoteScore)) continue;
          if (comparableScore(remoteScore) !== originalScore) {
            conflict = true;
          } else {
            merged[itemId] = localScore;
            dirty = true;
          }
        }
        mergedScores[rubricId] = merged;
      }
      Object.assign(form, buildRubricValues(rubrics, server.checks, mergedScores));
    } else {
      conflict = true;
    }
  }
  return { form, dirty, conflict };
}

// Tìm điểm tối đa theo đúng rubric và mục, dùng 0 nếu mục không còn tồn tại.
function findItemMaxPoints(rubrics: Rubric[], rubricId: string, itemId: string): number {
  return Number(
    rubrics
      .find((r) => r.rubricId === rubricId)
      ?.criteria.flatMap((c) => c.items)
      .find((i) => i.itemId === itemId)?.points ?? 0,
  );
}

// Cập nhật điểm và đồng bộ checklist khi mức đạt thay đổi.
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

// Đổi phần tổng hợp và tạo cấu trúc rỗng nếu bản chấm chưa có phản hồi.
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

// Cập nhật phản hồi của tiêu chí theo cặp ID rubric và mục.
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

// Đổi xác nhận mà vẫn giữ điểm gốc của nhận xét nếu giảng viên bỏ xác nhận trước khi lưu.
export function buildToggleAcknowledgePatch(
  form: GradingFormValues,
  key: string,
): Partial<GradingFormValues> | null {
  const [rubricId, itemId] = key.includes(":") ? (key.split(":", 2) as [string, string]) : [key, ""];
  if (!rubricId || !itemId) return null;
  const existingAck = new Set(form.acknowledgedReviewItems?.[rubricId] ?? []);
  const willAck = !existingAck.has(itemId);
  if (willAck) existingAck.add(itemId);
  else existingAck.delete(itemId);
  return {
    acknowledgedReviewItems: { ...(form.acknowledgedReviewItems ?? {}), [rubricId]: [...existingAck] },
  };
}

// Đưa checklist, điểm và phản hồi đề xuất vào form cục bộ, gắn ID để kiểm lúc lưu.
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
  for (const { rubricId, itemId } of retainedFeedback?.items ?? []) {
    const score = structuredFeedback
      ? normalized.itemScores[rubricId]?.[itemId]
      : form.reviewedItemScores?.[rubricId]?.[itemId] ?? form.itemScores?.[rubricId]?.[itemId];
    if (score !== undefined) (reviewedItemScores[rubricId] ??= {})[itemId] = score;
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

export function buildProposalReviewPatch(
  form: GradingFormValues,
  feedback?: StructuredFeedback | null,
): Partial<GradingFormValues> {
  const acknowledgedReviewItems = { ...form.acknowledgedReviewItems };
  for (const { rubricId, itemId, requiresReview } of feedback?.items ?? []) {
    const score = form.itemScores?.[rubricId]?.[itemId];
    const acknowledged = acknowledgedReviewItems[rubricId] ?? [];
    if (!requiresReview || score === undefined || acknowledged.includes(itemId)) continue;
    acknowledgedReviewItems[rubricId] = [...acknowledged, itemId];
  }
  return { acknowledgedReviewItems };
}

export function buildReviewedScoresForSave(form: GradingFormValues): ItemScores {
  const reviewedItemScores = { ...form.reviewedItemScores };
  for (const [rubricId, itemIds] of Object.entries(form.acknowledgedReviewItems ?? {})) {
    const scores = { ...reviewedItemScores[rubricId] };
    for (const itemId of itemIds) {
      const score = form.itemScores?.[rubricId]?.[itemId];
      if (score !== undefined) scores[itemId] = score;
    }
    reviewedItemScores[rubricId] = scores;
  }
  return reviewedItemScores;
}

export function listFeedbackScoreReviewItems(form: GradingFormValues, evaluation: Evaluation) {
  const savedItems = new Set(evaluation.structuredFeedback?.items.map(
    ({ rubricId, itemId }) => `${rubricId}:${itemId}`,
  ));
  const items = new Set<string>();
  for (const { rubricId, itemId } of form.structuredFeedback?.items ?? []) {
    const score = form.itemScores?.[rubricId]?.[itemId];
    if (score === undefined || form.acknowledgedReviewItems?.[rubricId]?.includes(itemId)) continue;
    const reviewedScore = form.reviewedItemScores?.[rubricId]?.[itemId];
    const key = `${rubricId}:${itemId}`;
    if (reviewedScore === undefined ? savedItems.has(key) : Number(reviewedScore) !== Number(score)) {
      items.add(key);
    }
  }
  return items;
}

/** Mục rubric mà form còn đúng bằng giá trị AI đã điền; sửa tay mục nào thì mục đó rời khỏi tập. Khóa `rubricId:itemId`. */
export function listAiFilledItems(rubrics: Rubric[], form: GradingFormValues, proposal: Proposal | undefined): Set<string> {
  const filled = new Set<string>();
  if (!proposal?.proposal) return filled;
  const aiChecks = proposal.proposal.checks ?? {};
  const aiScores = proposal.proposal.itemScoresByRubric ?? {};
  for (const rubric of rubrics) {
    for (const item of rubric.criteria.flatMap((criterion) => criterion.items)) {
      const hasAiItem = Object.prototype.hasOwnProperty.call(aiScores[rubric.rubricId] ?? {}, item.itemId)
        || Object.prototype.hasOwnProperty.call(aiChecks, rubric.rubricId);
      const max = Number(item.points);
      if (hasAiItem && resolveItemPoints(rubric.rubricId, item.itemId, max, form.checks, form.itemScores)
        === resolveItemPoints(rubric.rubricId, item.itemId, max, aiChecks, aiScores)) {
        filled.add(`${rubric.rubricId}:${item.itemId}`);
      }
    }
  }
  return filled;
}

/** Nối checklist với biểu mẫu chấm điểm. */
export function buildChecklistBindings(values: GradingFormValues, disabled: boolean, onChange: OnChange): ChecklistProps {
  // Xóa xác nhận xem lại của mục khi người chấm đã thay đổi điểm mục đó.
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
    // Chuyển checklist mới thành bản cập nhật từng phần của form.
    onChange: (checks) => onChange({ checks }),
    disabled,
    itemScores: values.itemScores,
    onScoresChange: handleScoresChange,
  };
}
