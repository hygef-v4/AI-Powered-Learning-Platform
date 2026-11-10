"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { gradingApi } from "../api";
import { buildRubricValues, hasPartialPoints, hasInvalidItemScores } from "../model/rubricScores";
import { formatErrorMessage } from "../model/format";
import { useGradingProposals } from "./useGradingProposals";
import { buildEvaluationHref, type NavigationFilters } from "../model/navigation";
import type {
  AiRequest,
  Checks,
  Detail,
  Evaluation,
  GradingFormValues,
  ItemScores,
  Proposal,
  Rubric,
  StructuredFeedback,
} from "../model/types";

const UNSAVED_CHANGES_MESSAGE = "Bạn có thay đổi chưa lưu. Rời bài này?";

/** Opening a grade preserves explicit item scores and historical adjustments. */
function createInitialForm(evaluation: Evaluation, rubrics: Rubric[]): GradingFormValues {
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
    adjustmentReason: evaluation.adjustmentReason ?? "",
    feedback: evaluation.feedback ?? "",
    changeReason: "",
    usedProposalId: null,
  };
}

export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>, filters: NavigationFilters = {}) {
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => createInitialForm(initial.evaluation, initial.rubrics));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [dirty, setDirty] = useState(false);
  const evaluation = detail.evaluation;
  const official = evaluation.state === "FINALIZED" || evaluation.state === "PUBLISHED";
  const disabled = busy || !detail.current || evaluation.state === "SUPERSEDED" || official;
  const dirtyRef = useRef(dirty);
  const { proposals, proposalsLoaded, pollExhausted, addProposal, resumePolling } = useGradingProposals(evaluation.id, busy, setError);

  useEffect(() => {
    dirtyRef.current = dirty;
  }, [dirty]);
  useEffect(() => {
    function beforeUnload(event: BeforeUnloadEvent) {
      if (dirtyRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    }
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, []);

  function updateForm(patch: Partial<GradingFormValues>) {
    if (disabled) return;
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
  }

  async function reloadEvaluation() {
    try {
      // Preserve unsaved edits only while grading the same submitted receipt.
      const refreshed = await refresh();
      setDetail(refreshed);
      const receiptChanged = refreshed.evaluation.submissionHash !== evaluation.submissionHash;
      if (receiptChanged || refreshed.evaluation.state === "FINALIZED" || refreshed.evaluation.state === "PUBLISHED") {
        setForm(createInitialForm(refreshed.evaluation, refreshed.rubrics));
        setDirty(false);
        dirtyRef.current = false;
      }
      if (receiptChanged) resumePolling();
    } catch (error) {
      setError(formatErrorMessage(error));
    }
  }

  function confirmNavigation() {
    return !dirty || window.confirm(UNSAVED_CHANGES_MESSAGE);
  }
  function navigateEvaluation(id: string, keepFilters = true) {
    if (confirmNavigation()) router.push(buildEvaluationHref(id, keepFilters ? filters : {}));
  }

  async function saveEvaluation(advance = false): Promise<Evaluation | null> {
    if (disabled) return null;
    const currentIndex = detail.navigationIds.indexOf(evaluation.id);
    const next = currentIndex >= 0 ? detail.navigationIds[currentIndex + 1] : undefined;
    setBusy(true);
    setError("");
    try {
      if (hasInvalidItemScores(detail.rubrics, form.itemScores)) {
        throw new Error("Kiểm tra điểm từng mục: từ 0 đến điểm tối đa, với tối đa 2 chữ số thập phân.");
      }
      const supportsItemScores = evaluation.rubricItemScores !== undefined;
      if (!supportsItemScores && hasPartialPoints(detail.rubrics, form.checks, form.itemScores)) {
        throw new Error("Máy chủ chưa hỗ trợ lưu điểm lẻ. Cần cập nhật máy chủ rồi tải lại phiên bản; điểm bạn đang nhập vẫn được giữ.");
      }
      const hasStructuredPayload = Boolean(
        form.structuredFeedback ||
        form.clearStructuredFeedback ||
        (form.acknowledgedReviewItems && Object.keys(form.acknowledgedReviewItems).length > 0) ||
        (form.reviewedItemScores && Object.keys(form.reviewedItemScores).length > 0),
      );
      const saved = await gradingApi.saveEvaluation(evaluation.id, {
        ...(supportsItemScores ? { itemScoresByRubric: form.itemScores ?? {} } : {}),
        ...(hasStructuredPayload
          ? {
            ...(form.structuredFeedback ? { structuredFeedback: form.structuredFeedback } : {}),
            ...(form.clearStructuredFeedback ? { clearStructuredFeedback: true } : {}),
            ...(form.acknowledgedReviewItems && Object.keys(form.acknowledgedReviewItems).length > 0
              ? { acknowledgedReviewItems: form.acknowledgedReviewItems }
              : {}),
            ...(form.reviewedItemScores && Object.keys(form.reviewedItemScores).length > 0
              ? { reviewedItemScores: form.reviewedItemScores }
              : {}),
          }
          : {}),
        version: evaluation.version,
        checkedItemsByRubric: form.checks,
        adjustment: form.adjustment,
        adjustmentReason: form.adjustmentReason || null,
        feedback: form.feedback,
        changeReason: form.changeReason || null,
        usedProposalId: form.usedProposalId,
      });
      if (saved) {
        setDetail({ ...detail, evaluation: saved });
        setForm((current) => ({
          ...current,
          structuredFeedback: saved.structuredFeedback ?? current.structuredFeedback ?? null,
          clearStructuredFeedback: false,
          acknowledgedReviewItems: saved.acknowledgedReviewItems ?? current.acknowledgedReviewItems ?? {},
          reviewedItemScores: saved.reviewedItemScores ?? current.reviewedItemScores ?? {},
          usedProposalId: null,
        }));
        setDirty(false);
        dirtyRef.current = false;
        if (advance && next) router.push(buildEvaluationHref(next, filters));
        return saved;
      }
      return null;
    } catch (error) {
      setError(formatErrorMessage(error));
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function requestProposal(input: AiRequest): Promise<Proposal | null> {
    if (disabled) return null;
    setBusy(true);
    setError("");
    try {
      const created = await gradingApi.requestProposal(evaluation.id, input);
      if (created) addProposal(created);
      return created ?? null;
    } catch (error) {
      setError(formatErrorMessage(error));
      return null;
    } finally {
      setBusy(false);
    }
  }

  /** Applying a proposal changes only the local draft and requires a fresh review. */
  function applyProposal(
    checks: Checks,
    feedback: string,
    proposalId: string,
    itemScores: ItemScores = {},
    structuredFeedback?: StructuredFeedback | null,
  ) {
    if (disabled) return;
    const normalized = buildRubricValues(detail.rubrics, checks, itemScores);
    const retainedFeedback = structuredFeedback ?? form.structuredFeedback ?? evaluation.structuredFeedback ?? null;
    const reviewedItemScores: ItemScores = {};
    // Legacy proposals contain no new explanations: keep only the old text's item/score baselines.
    if (!structuredFeedback && retainedFeedback) {
      for (const { rubricId, itemId } of retainedFeedback.items) {
        const score = form.reviewedItemScores?.[rubricId]?.[itemId] ?? form.itemScores?.[rubricId]?.[itemId];
        if (score !== undefined) (reviewedItemScores[rubricId] ??= {})[itemId] = score;
      }
    }
    updateForm({
      checks: normalized.checks,
      itemScores: normalized.itemScores,
      feedback,
      structuredFeedback: retainedFeedback,
      clearStructuredFeedback: false,
      acknowledgedReviewItems: {},
      reviewedItemScores,
      usedProposalId: proposalId,
    });
  }

  return {
    detail,
    evaluation,
    form,
    busy,
    error,
    dirty,
    proposals,
    proposalsLoaded,
    pollExhausted,
    disabled,
    official,
    currentIndex: detail.navigationIds.indexOf(evaluation.id),
    updateForm,
    reload: reloadEvaluation,
    navigate: navigateEvaluation,
    confirmNavigation,
    save: saveEvaluation,
    requestProposal,
    resumePolling,
    applyProposal,
  };
}
