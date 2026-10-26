"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { gradingApi } from "./api";
import { hasPartialPoints, invalidItemScores, itemPoints } from "./RubricChecklistPanel";
import { message } from "./useResource";
import { useGradingProposals } from "./useGradingProposals";
import type {
  AiRequest,
  Checks,
  Detail,
  Evaluation,
  GradingFormValues,
  ItemScores,
  Rubric,
  StructuredFeedback,
} from "./types";

const UNSAVED_CHANGES_MESSAGE = "Bạn có thay đổi chưa lưu. Rời bài này?";

function buildReviewedScores(rubrics: Rubric[], checks: Checks, itemScores: ItemScores = {}): ItemScores {
  const result: ItemScores = {};
  for (const rubric of rubrics) {
    const perRubric: Record<string, string | number> = {};
    for (const criterion of rubric.criteria) {
      for (const item of criterion.items) {
        const val = itemPoints(rubric.rubricId, item.itemId, Number(item.points), checks, itemScores);
        perRubric[item.itemId] = Number(val).toFixed(2);
      }
    }
    result[rubric.rubricId] = perRubric;
  }
  return result;
}

function initialForm(evaluation: Evaluation): GradingFormValues {
  let checks: Checks;
  try {
    checks = JSON.parse(evaluation.rubricChecks);
  } catch {
    checks = {};
  }
  return {
    checks,
    itemScores: evaluation.rubricItemScores ?? {},
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

export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>) {
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => initialForm(initial.evaluation));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [dirty, setDirty] = useState(false);
  const evaluation = detail.evaluation;
  const dirtyRef = useRef(dirty);
  const { proposals, pollExhausted, addProposal, resumePolling } = useGradingProposals(evaluation.id, busy, setError);

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
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
  }

  async function reload() {
    try {
      // Keep the unsaved form so a version conflict does not discard the teacher's work.
      setDetail(await refresh());
    } catch (error) {
      setError(message(error));
    }
  }

  function confirmNavigation() {
    return !dirty || window.confirm(UNSAVED_CHANGES_MESSAGE);
  }
  function navigate(id: string) {
    if (confirmNavigation()) router.push(`/teaching/evaluations/${id}`);
  }

  async function save() {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      if (invalidItemScores(detail.rubrics, form.itemScores)) {
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
      const saved = await gradingApi.save(evaluation.id, {
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
      setDetail({ ...detail, evaluation: saved });
      setDirty(false);
      dirtyRef.current = false;
      setForm((current) => ({
        ...current,
        structuredFeedback: saved.structuredFeedback ?? current.structuredFeedback ?? null,
        clearStructuredFeedback: false,
        acknowledgedReviewItems: saved.acknowledgedReviewItems ?? current.acknowledgedReviewItems ?? {},
        reviewedItemScores: saved.reviewedItemScores ?? current.reviewedItemScores ?? {},
        usedProposalId: null,
      }));
      const next = detail.navigationIds[detail.navigationIds.indexOf(evaluation.id) + 1];
      if (next) router.push(`/teaching/evaluations/${next}`);
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  async function requestProposal(input: AiRequest) {
    setBusy(true);
    setError("");
    try {
      addProposal(await gradingApi.request(evaluation.id, input));
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  function applyProposal(
    checks: Checks,
    feedback: string,
    proposalId: string,
    itemScores: ItemScores = {},
    structuredFeedback: StructuredFeedback | null = null,
  ) {
    const nextReviewed = structuredFeedback
      ? buildReviewedScores(detail.rubrics, checks, itemScores)
      : {};
    updateForm({
      checks,
      itemScores,
      feedback,
      structuredFeedback,
      clearStructuredFeedback: false,
      acknowledgedReviewItems: {},
      reviewedItemScores: nextReviewed,
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
    pollExhausted,
    disabled: busy || !detail.current || evaluation.state === "SUPERSEDED",
    official: evaluation.state === "FINALIZED" || evaluation.state === "PUBLISHED",
    currentIndex: detail.navigationIds.indexOf(evaluation.id),
    updateForm,
    reload,
    navigate,
    confirmNavigation,
    save,
    requestProposal,
    resumePolling,
    applyProposal,
  };
}
