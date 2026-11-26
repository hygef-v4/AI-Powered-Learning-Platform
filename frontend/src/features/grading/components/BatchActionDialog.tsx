"use client";
import { useEffect, useState } from "react";
import { Alert, Button, Dialog, showToast } from "@/shared/ui";
import { buildAiRequest } from "./AiProposalPanel";
import type { SourceChoice } from "@/shared/ai";
import { gradingApi } from "../api";
import { useGradingAiRequest } from "../hooks/useGradingAiRequest";
import { readGradingAiAccount, readGradingAiSessionRevision, resumeGradingPolling } from "../model/gradingAiTasks";
import { formatErrorMessage } from "../model/format";
import type { BatchResult, ItemVersion } from "../model/types";
import { msg } from "@/i18n/msg";
import { formatNumber } from "@/i18n/number";

const OUTCOME_CODE_MESSAGES: Record<string, string> = {
  ALREADY_FINALIZED: msg("grading.components.BatchActionDialog.thisSubmissionsGradeWasAlready"),
  ALREADY_PUBLISHED: msg("grading.components.BatchActionDialog.thisSubmissionsGradeWasAlready2"),
  INVALID_GRADE_STATE: msg("grading.components.BatchActionDialog.theSubmissionStatusHasChanged"),
  GRADE_VERSION_CONFLICT: msg("grading.components.BatchActionDialog.theGradingWasUpdatedReload"),
  GRADING_CLASS_READ_ONLY: msg("grading.components.BatchActionDialog.theClassIsArchivedOr"),
  GRADING_ASSIGNMENT_OPEN: msg("grading.components.BatchActionDialog.theAssignmentIsStillAccepting"),
  DUPLICATE_ITEM: msg("grading.components.BatchActionDialog.aSubmissionWasSelectedTwice"),
  INCOMPLETE_BATCH_RESULT: msg("grading.components.BatchActionDialog.notAllResultsWereReceived"),
  STRUCTURED_FEEDBACK_REVIEW_REQUIRED:
    msg("grading.components.BatchActionDialog.someFeedbackItemsStillNeed"),
  STRUCTURED_FEEDBACK_SCORE_MISMATCH:
    msg("grading.components.BatchActionDialog.someItemsHadTheirPoints"),
};

// Phân biệt bản đã hoàn tất từ trước với các trường hợp bị bỏ qua khác.
function isAlreadyCompleted(row: BatchResult["items"][number]) {
  return row.outcome === "SKIPPED" && (row.code === "ALREADY_FINALIZED" || row.code === "ALREADY_PUBLISHED");
}

// Hiện thao tác theo lô và kết quả riêng cho mỗi bản chấm được chọn.
export function BatchActionDialog({
  action,
  items,
  onClose,
  onDone,
}: {
  action: "finalize" | "publish" | "ai";
  items: ItemVersion[];
  onClose: () => void;
  onDone: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<BatchResult | null>(null);
  /** Đã chốt / công bố xong: thông báo nổi chung của trang thay cho popup. */
  const [completed, setCompleted] = useState(false);
  const [error, setError] = useState("");
  const [estimate, setEstimate] = useState<{ key: string; state: "ready" | "failed"; tokens?: number }>({
    key: "",
    state: "failed",
  });
  const itemsKey = JSON.stringify(items);
  const submitAiRequest = useGradingAiRequest(`${action}:${itemsKey}`);
  useEffect(() => {
    if (action !== "ai" || items.length === 0) return;
    let active = true;
    void gradingApi.estimateProposalTokens(items).then(
      (value) => {
        if (active) setEstimate({ key: itemsKey, state: "ready", tokens: value.estimatedTokens });
      },
      () => {
        if (active) setEstimate({ key: itemsKey, state: "failed" });
      },
    );
    return () => {
      active = false;
    };
  }, [action, items, itemsKey]);
  // Chạy nhánh được chọn và chỉ theo dõi kết quả AI trong đúng phiên tài khoản.
  async function runBatch(choice?: SourceChoice) {
    if (busy || items.length === 0) return;
    setBusy(true);
    setError("");
    const sessionRevision = readGradingAiSessionRevision();
    try {
      let completed: BatchResult;
      if (action === "ai") {
        completed = await submitAiRequest(buildAiRequest(choice), (request) =>
          gradingApi.requestProposals(items, request));
        if (sessionRevision !== readGradingAiSessionRevision()) return;
        if (readGradingAiAccount()) {
          completed.items.filter((item) => item.outcome === "PENDING" || item.outcome === "QUEUED")
            .forEach((item) => resumeGradingPolling(item.evaluationId));
        }
      } else {
        const response = action === "finalize" ? await gradingApi.finalizeGrades(items) : await gradingApi.publishGrades(items);
        const initial = { items: await Promise.all(response.items.map(async (row) => {
          if (row.outcome !== "FAILED" || row.code !== "INVALID_GRADE_STATE") return row;
          try {
            const { evaluation } = await gradingApi.loadEvaluation(row.evaluationId);
            if (evaluation.state === "PUBLISHED" || (action === "finalize" && evaluation.state === "FINALIZED")) {
              return { ...row, outcome: "SKIPPED", code: evaluation.state === "PUBLISHED" ? "ALREADY_PUBLISHED" : "ALREADY_FINALIZED" };
            }
          } catch {
            // An unavailable fresh read must not trigger an automatic grade write.
          }
          return row;
        })) };
        completed = initial;
      }
      setResult(completed);
      // Snapshot success before refreshing: a filtered list can remove the completed rows.
      if (action !== "ai" && completed.items.length === items.length
        && items.every((item) => completed.items.some((row) => row.evaluationId === item.evaluationId && (row.outcome === "SUCCESS" || isAlreadyCompleted(row))))) {
        const skipped = completed.items.filter(isAlreadyCompleted).length;
        const changed = items.length - skipped;
        const message = changed > 0
          ? action === "publish" ? msg("grading.components.BatchActionDialog.gradesPublishedForSubmissions", { changed: String(changed) }) : msg("grading.components.BatchActionDialog.gradesFinalizedForSubmissions", { changed: String(changed) })
          : msg("grading.components.BatchActionDialog.theSubmissionsWereAlreadyFinalized");
        showToast(message + (skipped > 0 && changed > 0 ? msg("grading.components.BatchActionDialog.submissionsWereAlreadyCompletedTheir", { skipped: String(skipped) }) : ""));
        setCompleted(true);
        onClose();
      }
      onDone();
    } catch (e) {
      if (action !== "ai" || sessionRevision === readGradingAiSessionRevision()) {
        setError(formatErrorMessage(e));
      }
    } finally {
      setBusy(false);
    }
  }
  const title =
    action === "publish"
      ? msg("grading.components.BatchActionDialog.publishGrades")
      : action === "finalize"
        ? msg("grading.components.BatchActionDialog.finalizeGrades")
        : msg("grading.components.BatchActionDialog.batchAiProposals");
  const estimateIsCurrent = estimate.key === itemsKey;
  const estimateState = estimateIsCurrent ? estimate.state : "loading";
  if (completed) return null;
  return (
    <Dialog
      open
      title={title}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          {result ? <Button onClick={onClose}>{msg("grading.components.BatchActionDialog.close")}</Button> : <Button
            variant="primary"
            disabled={busy || items.length === 0}
            onClick={() => void runBatch()}
          >
            {busy ? msg("grading.components.BatchActionDialog.processing") : msg("grading.components.BatchActionDialog.confirm")}
          </Button>}
        </>
      }
    >
      {/* Popup công bố điểm bỏ phần nội dung (yêu cầu 2026-11-07). */}
      {action !== "publish" && (
        <p className="font-display text-[34px] leading-none text-ink">{items.length} <span className="font-sans text-[13.5px] text-muted">{msg("grading.components.BatchActionDialog.selectedSubmissions")}</span></p>
      )}
      {/* Nguồn AI do hệ thống tự chọn: API chung nếu còn đủ hạn mức cho lượt này, không thì khóa cá nhân (yêu cầu 2026-11-05). */}
      {action === "ai" && (
        <p className="text-xs text-muted">
          {estimateState === "loading" ? msg("grading.components.BatchActionDialog.estimatingTokens") : estimateState === "ready" && estimate.tokens != null
            ? msg("grading.components.BatchActionDialog.estimatedAboutTokens", { value: String(formatNumber(estimate.tokens)) }) : msg("grading.components.BatchActionDialog.couldNotEstimateTokens")}
        </p>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {result && (
        <ul className="flex flex-col">
          {result.items.map((r, index) => {
            const detailMsg = r.code ? OUTCOME_CODE_MESSAGES[r.code] ?? msg("grading.components.BatchActionDialog.couldNotPerformTheAction") : undefined;
            return (
              <li key={`${r.evaluationId}-${index}`} className="border-b border-line py-2 last:border-b-0">
                <p>
                  {msg("grading.components.BatchActionDialog.submission")}{index + 1}: {r.outcome === "SUCCESS" ? msg("grading.components.BatchActionDialog.succeeded") : r.outcome === "QUEUED" || r.outcome === "PENDING" ? msg("grading.components.BatchActionDialog.aiRequestSent") : r.outcome === "READY" ? msg("grading.components.BatchActionDialog.aiProposalAvailable") : r.outcome === "SKIPPED" ? msg("grading.components.BatchActionDialog.skipped") : msg("grading.components.BatchActionDialog.notDoneYet")}
                </p>
                {detailMsg && (
                  <p role={r.outcome === "FAILED" ? "alert" : undefined}
                    className={`mt-1 text-xs font-medium ${r.outcome === "FAILED" ? "text-danger" : "text-muted"}`}>
                    {detailMsg}
                  </p>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Dialog>
  );
}
