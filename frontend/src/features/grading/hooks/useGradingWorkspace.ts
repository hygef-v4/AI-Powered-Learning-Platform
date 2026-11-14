"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useConfirmDialog } from "@/shared/ui/ConfirmDialog";
import { gradingApi } from "../api";
import { hasPartialPoints, hasInvalidItemScores } from "../model/rubricScores";
import { formatErrorMessage } from "../model/format";
import { useGradingProposals } from "./useGradingProposals";
import { readGradingAiAccount } from "../model/gradingAiTasks";
import { buildEvaluationHref, type NavigationFilters } from "../model/navigation";
import {
  buildItemScorePatch,
  buildProposalDraftPatch,
  buildStructuredItemPatch,
  buildStructuredSummaryPatch,
  buildToggleAcknowledgePatch,
  createInitialForm,
} from "../components/GradingForm";
import type {
  AiRequest,
  Checks,
  Detail,
  Evaluation,
  GradingFormValues,
  ItemScores,
  Proposal,
  StructuredFeedback,
  StructuredFeedbackItem,
  StructuredFeedbackSummary,
} from "../model/types";

const UNSAVED_CHANGES_MESSAGE = "Bạn có thay đổi chưa lưu. Rời bài này?";

export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>,
  filters: NavigationFilters = {}, additionalDirty = false) {
  const [confirmDialog, requestConfirm] = useConfirmDialog();
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => createInitialForm(initial.evaluation, initial.rubrics));
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const requestingRef = useRef(false);
  const editRevision = useRef(0);
  const [error, setError] = useState("");
  const [dirty, setDirty] = useState(false);
  const [aiAutoApplied, setAiAutoApplied] = useState(false);
  const evaluation = detail.evaluation;
  const official = evaluation.state === "FINALIZED" || evaluation.state === "PUBLISHED";
  const disabled = busy || !detail.current || detail.editable === false || evaluation.state === "SUPERSEDED";
  const dirtyRef = useRef(dirty);
  const additionalDirtyRef = useRef(additionalDirty);
  const { proposals, proposalsLoaded, pollExhausted, addProposal, resumePolling } = useGradingProposals(evaluation.id, busy, setError);

  useEffect(() => {
    dirtyRef.current = dirty;
    additionalDirtyRef.current = additionalDirty;
  }, [dirty, additionalDirty]);
  useEffect(() => {
    function beforeUnload(event: BeforeUnloadEvent) {
      if (dirtyRef.current || additionalDirtyRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    }
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, []);

  function updateForm(patch: Partial<GradingFormValues>) {
    if (disabled) return;
    editRevision.current += 1;
    setError("");
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
    setAiAutoApplied(false);
  }

  function toggleItem(rubricId: string, itemId: string, achieved: boolean) {
    const max = Number(
      detail.rubrics
        .find((r) => r.rubricId === rubricId)
        ?.criteria.flatMap((c) => c.items)
        .find((i) => i.itemId === itemId)?.points ?? 0,
    );
    updateForm(buildItemScorePatch(detail.rubrics, form, rubricId, itemId, achieved ? max : 0));
  }

  function setItemScore(rubricId: string, itemId: string, score: number) {
    updateForm(buildItemScorePatch(detail.rubrics, form, rubricId, itemId, score));
  }

  function setFeedback(feedback: string) {
    updateForm({ feedback });
  }
  function setAdjustment(adjustment: string) {
    updateForm({ adjustment });
  }
  function setChangeReason(changeReason: string) {
    updateForm({ changeReason });
  }

  function updateStructuredSummary(summary: StructuredFeedbackSummary) {
    updateForm(buildStructuredSummaryPatch(form, summary));
  }

  function updateStructuredItem(rubricId: string, itemId: string, patch: Partial<StructuredFeedbackItem>) {
    updateForm(buildStructuredItemPatch(detail.rubrics, form, rubricId, itemId, patch));
  }

  function toggleAcknowledgedReviewItem(key: string) {
    const patch = buildToggleAcknowledgePatch(detail.rubrics, form, key);
    if (patch) updateForm(patch);
  }

  async function reloadEvaluation() {
    try {
      const refreshed = await refresh();
      setDetail(refreshed);
      setError("");
      const receiptChanged = refreshed.evaluation.submissionHash !== evaluation.submissionHash;
      if (receiptChanged || refreshed.editable === false || refreshed.evaluation.state === "SUPERSEDED") {
        setForm(createInitialForm(refreshed.evaluation, refreshed.rubrics));
        setDirty(false);
        setAiAutoApplied(false);
        dirtyRef.current = false;
      }
      if (receiptChanged) resumePolling();
    } catch (error) {
      setError(formatErrorMessage(error));
    }
  }

  function navigateEvaluation(id: string, keepFilters = true) {
    const go = () => router.push(buildEvaluationHref(id, keepFilters ? filters : {}));
    if (dirty || additionalDirty) requestConfirm(UNSAVED_CHANGES_MESSAGE, go, "Bỏ thay đổi");
    else go();
  }

  async function saveEvaluation(advance = false,
    options: { background?: boolean } = {}): Promise<Evaluation | null> {
    if (disabled || savingRef.current) return null;
    const currentIndex = detail.navigationIds.indexOf(evaluation.id);
    const next = currentIndex >= 0 ? detail.navigationIds[currentIndex + 1] : undefined;
    const savedRevision = editRevision.current;
    savingRef.current = true;
    setSaving(true);
    if (!options.background) setBusy(true);
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
            ...(form.structuredFeedback ? {
              structuredFeedback: {
                ...form.structuredFeedback,
                summary: {
                  ...form.structuredFeedback.summary,
                  strengths: form.structuredFeedback.summary.strengths.filter((entry) => entry.trim()),
                  improvements: form.structuredFeedback.summary.improvements.filter((entry) => entry.trim()),
                },
              },
            } : {}),
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
        adjustmentReason: Number(form.adjustment) !== 0 ? form.changeReason.trim() || null : null,
        feedback: form.feedback,
        changeReason: form.changeReason.trim() || null,
        usedProposalId: form.usedProposalId,
      });
      if (saved) {
        setDetail((current) => ({ ...current, evaluation: saved }));
        let replacementForm: GradingFormValues | null = null;
        if (saved.kind === "GROUP_DOCUMENT") {
          try {
            const refreshed = await refresh();
            setDetail(refreshed);
            if (refreshed.evaluation.submissionHash !== saved.submissionHash
                || refreshed.editable === false || refreshed.evaluation.state === "SUPERSEDED") {
              replacementForm = createInitialForm(refreshed.evaluation, refreshed.rubrics);
            }
          } catch {
            setDetail({ ...detail, evaluation: saved, members: [] });
            setError("Đã lưu điểm nhóm. Tải lại bài để cập nhật điểm và phiên bản thành viên.");
          }
        }
        const preserveEdits = !replacementForm && editRevision.current !== savedRevision;
        setForm((current) => replacementForm ?? (preserveEdits ? {
          ...current,
          usedProposalId: current.usedProposalId === form.usedProposalId ? null : current.usedProposalId,
        } : {
          ...current,
          adjustment: String(saved.adjustment ?? current.adjustment),
          structuredFeedback: saved.structuredFeedback ?? current.structuredFeedback ?? null,
          clearStructuredFeedback: false,
          acknowledgedReviewItems: saved.acknowledgedReviewItems ?? current.acknowledgedReviewItems ?? {},
          reviewedItemScores: saved.reviewedItemScores ?? current.reviewedItemScores ?? {},
          usedProposalId: null,
        }));
        setDirty(preserveEdits);
        if (!preserveEdits) setAiAutoApplied(false);
        dirtyRef.current = preserveEdits;
        if (advance && next && !preserveEdits) router.push(buildEvaluationHref(next, filters));
        return saved;
      }
      return null;
    } catch (error) {
      setError(formatErrorMessage(error));
      return null;
    } finally {
      savingRef.current = false;
      setSaving(false);
      if (!options.background) setBusy(false);
    }
  }

  async function requestProposal(input: AiRequest): Promise<Proposal | null> {
    if (disabled || savingRef.current || requestingRef.current) return null;
    requestingRef.current = true;
    const account = readGradingAiAccount();
    setError("");
    try {
      const created = await gradingApi.requestProposal(evaluation.id, input);
      if (account !== readGradingAiAccount()) return null;
      if (created) addProposal(created);
      return created ?? null;
    } catch (error) {
      setError(formatErrorMessage(error));
      return null;
    } finally {
      requestingRef.current = false;
    }
  }

  function applyProposal(
    checks: Checks,
    feedback: string,
    proposalId: string,
    itemScores: ItemScores = {},
    structuredFeedback?: StructuredFeedback | null,
  ) {
    if (disabled) return;
    editRevision.current += 1;
    const patch = buildProposalDraftPatch(
      detail.rubrics,
      evaluation,
      form,
      checks,
      feedback,
      proposalId,
      itemScores,
      structuredFeedback,
    );
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
    setAiAutoApplied(true);
  }

  return {
    detail,
    evaluation,
    form,
    busy,
    saving,
    error,
    dirty,
    aiAutoApplied,
    proposals,
    proposalsLoaded,
    pollExhausted,
    disabled,
    official,
    currentIndex: detail.navigationIds.indexOf(evaluation.id),
    updateForm,
    toggleItem,
    setItemScore,
    setFeedback,
    setAdjustment,
    setChangeReason,
    updateStructuredSummary,
    updateStructuredItem,
    toggleAcknowledgedReviewItem,
    reload: reloadEvaluation,
    navigate: navigateEvaluation,
    confirmDialog,
    save: saveEvaluation,
    requestProposal,
    resumePolling,
    applyProposal,
  };
}
