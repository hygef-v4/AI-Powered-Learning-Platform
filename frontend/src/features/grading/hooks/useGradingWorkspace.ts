"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useConfirmDialog } from "@/shared/ui/ConfirmDialog";
import { gradingApi } from "../api";
import { hasPartialPoints, hasInvalidItemScores } from "../model/rubricScores";
import { formatErrorMessage } from "../model/format";
import { useGradingAiRequest } from "./useGradingAiRequest";
import { useGradingProposals } from "./useGradingProposals";
import { readGradingAiSessionRevision } from "../model/gradingAiTasks";
import { buildEvaluationHref, type NavigationFilters } from "../model/navigation";
import {
  buildItemScorePatch,
  buildProposalDraftPatch,
  buildProposalReviewPatch,
  buildReviewedScoresForSave,
  buildStructuredItemPatch,
  buildStructuredSummaryPatch,
  buildToggleAcknowledgePatch,
  createInitialForm,
  rebaseGradingForm,
} from "../components/GradingForm";
import type {
  AiRequest,
  BatchResult,
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
import { msg } from "@/i18n/msg";

const UNSAVED_CHANGES_MESSAGE = msg("grading.hooks.useGradingWorkspace.youHaveUnsavedChangesLeave");

// Quản lý bản gốc, bản đang sửa và các hành động lưu hoặc áp dụng đề xuất AI.
export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>,
  filters: NavigationFilters = {}, additionalDirty = false) {
  const [confirmDialog, requestConfirm] = useConfirmDialog();
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => createInitialForm(initial.evaluation, initial.rubrics));
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const reloadingRef = useRef(false);
  const requestingRef = useRef(false);
  const editRevision = useRef(0);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [aiAutoApplied, setAiAutoApplied] = useState(false);
  /** Form trước khi AI điền, để "Hoàn tác" khôi phục; giữ bản gốc nếu AI điền nhiều lần liên tiếp. */
  const [aiBackup, setAiBackup] = useState<{ form: GradingFormValues; dirty: boolean } | null>(null);
  const evaluation = detail.evaluation;
  const submitAiRequest = useGradingAiRequest(
    JSON.stringify([evaluation.id, evaluation.submissionHash, detail.rubrics]),
  );
  const official = evaluation.state === "FINALIZED" || evaluation.state === "PUBLISHED";
  const disabled = busy || conflict || !detail.current || detail.editable === false || evaluation.state === "SUPERSEDED";
  const dirtyRef = useRef(dirty);
  const additionalDirtyRef = useRef(additionalDirty);
  const { proposals, proposalsLoaded, pollExhausted, addProposal, resumePolling } = useGradingProposals(evaluation.id, busy, setError);

  useEffect(() => {
    dirtyRef.current = dirty;
    additionalDirtyRef.current = additionalDirty;
  }, [dirty, additionalDirty]);
  useEffect(() => {
    // Cảnh báo khi rời trang còn chỉnh sửa bản chấm hoặc thành viên chưa lưu.
    function warnBeforeUnload(event: BeforeUnloadEvent) {
      if (dirtyRef.current || additionalDirtyRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    }
    window.addEventListener("beforeunload", warnBeforeUnload);
    return () => window.removeEventListener("beforeunload", warnBeforeUnload);
  }, []);

  // Gộp chỉnh sửa thủ công và tăng revision để phản hồi lưu cũ không ghi đè.
  function updateForm(patch: Partial<GradingFormValues>) {
    if (disabled || reloadingRef.current) return;
    editRevision.current += 1;
    setError("");
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
    setAiAutoApplied(false);
  }

  // Đưa mục về điểm tối đa hoặc 0 và đồng bộ checklist.
  function toggleItem(rubricId: string, itemId: string, achieved: boolean) {
    const max = Number(
      detail.rubrics
        .find((r) => r.rubricId === rubricId)
        ?.criteria.flatMap((c) => c.items)
        .find((i) => i.itemId === itemId)?.points ?? 0,
    );
    updateForm(buildItemScorePatch(detail.rubrics, form, rubricId, itemId, achieved ? max : 0));
  }

  // Cập nhật điểm lẻ theo rubric và làm lại trạng thái liên quan của mục.
  function setItemScore(rubricId: string, itemId: string, score: number) {
    updateForm(buildItemScorePatch(detail.rubrics, form, rubricId, itemId, score));
  }

  // Ghi phản hồi chung vào bản cục bộ để lượt lưu gửi cùng điểm.
  function setFeedback(feedback: string) {
    updateForm({ feedback });
  }
  // Giữ giá trị điều chỉnh trong form trước khi kiểm và lưu.
  function setAdjustment(adjustment: string) {
    updateForm({ adjustment });
  }
  // Đưa lý do sửa điểm vào bản cục bộ để API nhận cùng thao tác chấm.
  function setChangeReason(changeReason: string) {
    updateForm({ changeReason });
  }

  // Cập nhật phần tổng hợp trong khi giữ phản hồi theo từng tiêu chí.
  function updateStructuredSummary(summary: StructuredFeedbackSummary) {
    updateForm(buildStructuredSummaryPatch(form, summary));
  }

  // Ghép chỉnh sửa vào đúng tiêu chí theo cặp ID.
  function updateStructuredItem(rubricId: string, itemId: string, patch: Partial<StructuredFeedbackItem>) {
    updateForm(buildStructuredItemPatch(detail.rubrics, form, rubricId, itemId, patch));
  }

  // Cập nhật xác nhận cần xem lại và điểm gắn với lần xác nhận đó.
  function toggleAcknowledgedReviewItem(key: string) {
    const patch = buildToggleAcknowledgePatch(form, key);
    if (patch) updateForm(patch);
  }

  /**
   * Ghi ngay version và trạng thái mà lượt công bố trả về, để lần lưu tiếp theo không bị xung đột phiên bản
   * khi lần tải lại sau công bố thất bại. Form đang sửa được giữ nguyên.
   */
  function applyPublishResult(result: BatchResult) {
    const row = result.items.find((item) => item.evaluationId === evaluation.id);
    if (row?.outcome !== "SUCCESS" || row.newVersion == null) return;
    const newVersion = row.newVersion;
    setDetail((current) => current.evaluation.id !== row.evaluationId || current.evaluation.version > newVersion
      ? current
      : { ...current, evaluation: { ...current.evaluation, version: newVersion, state: "PUBLISHED" } });
  }

  // Nạp bản hiện hành, đặt lại nếu bài nộp đổi hoặc hợp nhất khi chỉ version bản chấm thay đổi.
  async function reloadEvaluation() {
    if (savingRef.current || reloadingRef.current) return;
    reloadingRef.current = true;
    setBusy(true);
    try {
      const refreshed = await refresh();
      const receiptChanged = refreshed.evaluation.submissionHash !== evaluation.submissionHash;
      const reset = receiptChanged || !refreshed.current || refreshed.editable === false
        || refreshed.evaluation.state === "SUPERSEDED";
      if (!reset && refreshed.evaluation.version < evaluation.version) return;
      if (!reset && refreshed.evaluation.version === evaluation.version) {
        setDetail(refreshed);
        setError("");
        return;
      }
      const serverForm = createInitialForm(refreshed.evaluation, refreshed.rubrics);
      const rebased = reset || !dirtyRef.current
        ? { form: serverForm, dirty: false, conflict: false }
        : rebaseGradingForm(createInitialForm(evaluation, detail.rubrics), form, serverForm, refreshed.rubrics);
      // Chuyển bản hợp nhất vào form và đặt lại thăm dò nếu bài nộp đã đổi.
      const apply = () => {
        setDetail(refreshed);
        setForm(rebased.form);
        setDirty(rebased.dirty);
        dirtyRef.current = rebased.dirty;
        setAiAutoApplied(aiAutoApplied && rebased.form.usedProposalId !== null);
        if (rebased.form.usedProposalId === null) setAiBackup(null);
        setConflict(false);
        setError("");
        if (receiptChanged) resumePolling();
      };
      if (rebased.conflict) {
        setConflict(true);
        requestConfirm(msg("grading.hooks.useGradingWorkspace.resolveOverlappingEdits"), apply,
          msg("grading.hooks.useGradingWorkspace.useServerEdits"));
      } else {
        apply();
      }
    } catch (error) {
      setError(formatErrorMessage(error));
    } finally {
      reloadingRef.current = false;
      setBusy(false);
    }
  }

  // Hỏi xác nhận nếu còn chỉnh sửa và giữ bộ lọc khi chuyển bài nếu caller yêu cầu.
  function navigateEvaluation(id: string, keepFilters = true) {
    // Chuyển route với bộ lọc cũ khi thao tác điều hướng đã được phép.
    const go = () => router.push(buildEvaluationHref(id, keepFilters ? filters : {}));
    if (dirty || additionalDirty) requestConfirm(UNSAVED_CHANGES_MESSAGE, go, msg("grading.hooks.useGradingWorkspace.discardChanges"));
    else go();
  }

  // Gửi bản cục bộ theo version hiện hành và chỉ chuyển bài khi không còn chỉnh sửa mới chưa lưu.
  async function saveEvaluation(advance = false,
    options: { background?: boolean; forPublish?: boolean } = {}): Promise<Evaluation | null> {
    if (disabled || savingRef.current || reloadingRef.current) return null;
    const currentIndex = detail.navigationIds.indexOf(evaluation.id);
    const next = currentIndex >= 0 ? detail.navigationIds[currentIndex + 1] : undefined;
    const savedRevision = editRevision.current;
    savingRef.current = true;
    setSaving(true);
    if (!options.background) setBusy(true);
    setError("");
    try {
      if (hasInvalidItemScores(detail.rubrics, form.itemScores)) {
        throw new Error(msg("grading.hooks.useGradingWorkspace.checkEachItemsPointsFrom"));
      }
      const supportsItemScores = evaluation.rubricItemScores !== undefined;
      if (!supportsItemScores && hasPartialPoints(detail.rubrics, form.checks, form.itemScores)) {
        throw new Error(msg("grading.hooks.useGradingWorkspace.theServerDoesNotSupport"));
      }
      const reviewedItemScores = buildReviewedScoresForSave(form);
      const hasStructuredPayload = Boolean(
        form.structuredFeedback ||
        form.clearStructuredFeedback ||
        (form.acknowledgedReviewItems && Object.keys(form.acknowledgedReviewItems).length > 0) ||
        Object.keys(reviewedItemScores).length > 0,
      );
      // Gửi version của bản đang sửa. usedProposalId báo backend kiểm đề xuất khi lưu.
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
            ...(Object.keys(reviewedItemScores).length > 0
              ? { reviewedItemScores }
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
        let groupRefreshFailed = false;
        if (saved.kind === "GROUP_DOCUMENT") {
          try {
            const refreshed = await refresh();
            if (!refreshed.current || refreshed.evaluation.submissionHash !== saved.submissionHash
                || refreshed.editable === false || refreshed.evaluation.state === "SUPERSEDED") {
              setDetail(refreshed);
              replacementForm = createInitialForm(refreshed.evaluation, refreshed.rubrics);
            } else {
              setDetail({ ...refreshed, evaluation: saved });
            }
          } catch {
            groupRefreshFailed = true;
            setDetail((current) => ({ ...current, evaluation: saved, editable: false }));
            setError(msg("grading.hooks.useGradingWorkspace.groupScoreSavedReloadThe"));
          }
        }
        // Nếu giảng viên sửa tiếp trong lúc chờ lưu, giữ phần nhập mới để lần lưu sau gửi tiếp.
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
        if (!preserveEdits) {
          setAiAutoApplied(false);
          setAiBackup(null);
        }
        dirtyRef.current = preserveEdits;
        if (advance && next && !preserveEdits) router.push(buildEvaluationHref(next, filters));
        if (options.forPublish && (groupRefreshFailed || replacementForm || preserveEdits)) return null;
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

  // Chặn yêu cầu chồng lấn và đưa đề xuất vào UI nếu phiên tài khoản vẫn còn hiệu lực.
  async function requestProposal(input: AiRequest): Promise<Proposal | null> { // yêu cầu tạo đề xuất AI
    if (disabled || savingRef.current || reloadingRef.current || requestingRef.current) return null;
    requestingRef.current = true;
    const sessionRevision = readGradingAiSessionRevision();
    setError("");
    try {
      const created = await submitAiRequest(input, (request) =>
        gradingApi.requestProposal(evaluation.id, request));
      if (sessionRevision !== readGradingAiSessionRevision()) return null;
      if (created) addProposal(created);
      return created ?? null;
    } catch (error) {
      if (sessionRevision === readGradingAiSessionRevision()) setError(formatErrorMessage(error));
      return null;
    } finally {
      requestingRef.current = false;
    }
  }

  // Đưa đề xuất vào form và giữ bản sao để giảng viên có thể hoàn tác.
  function applyProposal(
    checks: Checks,
    feedback: string,
    proposalId: string,
    itemScores: ItemScores = {},
    structuredFeedback?: StructuredFeedback | null,
    options: { acceptReview?: boolean } = {},
  ) {
    if (disabled || reloadingRef.current) return;
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
    const review = options.acceptReview && structuredFeedback
      ? buildProposalReviewPatch({ ...form, ...patch })
      : {};
    setAiBackup((backup) => backup ?? { form, dirty });
    setForm((current) => ({ ...current, ...patch, ...review }));
    setDirty(true);
    setAiAutoApplied(true);
  }

  // Bỏ đề xuất đang dùng trong form sau khi xử lý xác nhận nếu cần.
  function discardProposal() {
    if (!aiBackup || disabled || reloadingRef.current) return;
    // Khôi phục dữ liệu và trạng thái chưa lưu từ trước khi áp dụng đề xuất.
    const restore = () => {
      editRevision.current += 1;
      setForm(aiBackup.form);
      setDirty(aiBackup.dirty);
      dirtyRef.current = aiBackup.dirty;
      setAiAutoApplied(false);
      setAiBackup(null);
    };
    // aiAutoApplied tắt khi giáo viên sửa tay sau lúc AI điền: hoàn tác sẽ mất cả phần sửa đó nên hỏi trước.
    if (aiAutoApplied) restore();
    else requestConfirm(msg("grading.hooks.useGradingWorkspace.undoDiscardsEdits"), restore, msg("grading.components.AiDraftBanner.undo"));
  }

  /** Chốt bản AI đã điền (chưa lưu): bỏ khả năng hoàn tác và cho autosave chạy lại. */
  function keepProposal() {
    const applied = proposals.find((proposal) => proposal.id === form.usedProposalId);
    if (applied?.proposal?.structuredFeedback && form.structuredFeedback?.items.some((item) => item.requiresReview)) {
      updateForm(buildProposalReviewPatch(form));
    } else {
      setAiAutoApplied(false);
    }
    setAiBackup(null);
  }

  return {
    detail,
    evaluation,
    form,
    busy,
    saving,
    error: conflict ? msg("grading.hooks.useGradingWorkspace.overlappingEditsRequireReview") : error,
    dirty,
    aiAutoApplied,
    hasAiBackup: aiBackup !== null,
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
    applyPublishResult,
    navigate: navigateEvaluation,
    confirmDialog,
    save: saveEvaluation,
    requestProposal,
    resumePolling,
    applyProposal,
    discardProposal,
    keepProposal,
  };
}
