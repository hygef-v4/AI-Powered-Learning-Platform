"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { gradingApi } from "./api";
import { checklistOnly, hasPartialPoints, invalidItemScores } from "./RubricChecklistPanel";
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
} from "./types";

const UNSAVED_CHANGES_MESSAGE = "Bạn có thay đổi chưa lưu. Rời bài này?";

/** Biểu mẫu chấm khi mở bài: điểm đưa về checklist (điểm lẻ cũ thành chưa đạt cho tới khi giảng viên tích). */
function initialForm(evaluation: Evaluation, rubrics: Rubric[]): GradingFormValues {
  let checks: Checks;
  try {
    checks = JSON.parse(evaluation.rubricChecks);
  } catch {
    checks = {};
  }
  const normalized = checklistOnly(rubrics, checks, evaluation.rubricItemScores ?? {});
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

export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>) {
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => initialForm(initial.evaluation, initial.rubrics));
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
        // Màn chấm không còn điều chỉnh ngoài rubric (BR-U09-20): điểm bài là tổng mục đạt.
        adjustment: "0",
        adjustmentReason: null,
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

  /**
   * Áp dụng đề xuất AI vào bản nháp: chỉ lấy checklist (mục AI cho đủ điểm là đạt, điểm lẻ thành chưa đạt). Tạm thời không
   * nhận nhận xét của AI; nhận xét cấu trúc cũ của bài (nếu có) được xoá khi lưu.
   */
  function applyProposal(
    checks: Checks,
    _feedback: string,
    proposalId: string,
    itemScores: ItemScores = {},
  ) {
    const normalized = checklistOnly(detail.rubrics, checks, itemScores);
    updateForm({
      checks: normalized.checks,
      itemScores: normalized.itemScores,
      structuredFeedback: null,
      clearStructuredFeedback: Boolean(form.structuredFeedback || evaluation.structuredFeedback),
      acknowledgedReviewItems: {},
      reviewedItemScores: {},
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
