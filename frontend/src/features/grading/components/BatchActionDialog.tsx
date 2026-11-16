"use client";
import { useEffect, useState } from "react";
import { Alert, Button, Dialog, showToast } from "@/shared/ui";
import { buildAiRequest } from "./AiProposalPanel";
import type { SourceChoice } from "@/shared/ai";
import { gradingApi } from "../api";
import { readGradingAiAccount, resumeGradingPolling } from "../model/gradingAiTasks";
import { formatErrorMessage } from "../model/format";
import type { BatchResult, ItemVersion } from "../model/types";
import { msg } from "@/i18n/msg";

const OUTCOME_CODE_MESSAGES: Record<string, string> = {
  ALREADY_FINALIZED: msg("grading.components.BatchActionDialog.baiNayDaDuocChotDiem"),
  ALREADY_PUBLISHED: msg("grading.components.BatchActionDialog.baiNayDaDuocCongBo"),
  INVALID_GRADE_STATE: msg("grading.components.BatchActionDialog.trangThaiBaiDaThayDoi"),
  GRADE_VERSION_CONFLICT: msg("grading.components.BatchActionDialog.baiChamDaDuocCapNhat"),
  GRADE_LOCKED: msg("grading.components.BatchActionDialog.diemDaDuocKhoaTaiLai"),
  GRADING_CLASS_READ_ONLY: msg("grading.components.BatchActionDialog.lopDaLuuTruHoacChua"),
  GRADING_ASSIGNMENT_OPEN: msg("grading.components.BatchActionDialog.baiTapDangNhanBaiDong"),
  DUPLICATE_ITEM: msg("grading.components.BatchActionDialog.baiBiChonTrungChiXu"),
  INCOMPLETE_BATCH_RESULT: msg("grading.components.BatchActionDialog.chuaNhanDuKetQuaXu"),
  STRUCTURED_FEEDBACK_REVIEW_REQUIRED:
    msg("grading.components.BatchActionDialog.conMucNhanXetCanGiang"),
  STRUCTURED_FEEDBACK_SCORE_MISMATCH:
    msg("grading.components.BatchActionDialog.coMucDaThayDoiDiem"),
};

function isAlreadyCompleted(row: BatchResult["items"][number]) {
  return row.outcome === "SKIPPED" && (row.code === "ALREADY_FINALIZED" || row.code === "ALREADY_PUBLISHED");
}

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
  useEffect(() => {
    if (action !== "ai" || items.length === 0) return;
    let active = true;
    const key = JSON.stringify(items);
    void gradingApi.estimateProposalTokens(items).then(
      (value) => {
        if (active) setEstimate({ key, state: "ready", tokens: value.estimatedTokens });
      },
      () => {
        if (active) setEstimate({ key, state: "failed" });
      },
    );
    return () => {
      active = false;
    };
  }, [action, items]);
  async function run(choice?: SourceChoice) {
    if (busy || items.length === 0) return;
    setBusy(true);
    setError("");
    const account = readGradingAiAccount();
    try {
      let completed: BatchResult;
      if (action === "ai") {
        completed = await gradingApi.requestProposals(items, buildAiRequest(choice));
        if (account && account === readGradingAiAccount()) {
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
          ? action === "publish" ? msg("grading.components.BatchActionDialog.daCongBoDiemChoBai", { changed: String(changed) }) : msg("grading.components.BatchActionDialog.daChotDiemChoBai", { changed: String(changed) })
          : msg("grading.components.BatchActionDialog.cacBaiDaDuocChotHoac");
        showToast(message + (skipped > 0 && changed > 0 ? msg("grading.components.BatchActionDialog.baiDaHoanTatTruocDo", { skipped: String(skipped) }) : ""));
        setCompleted(true);
        onClose();
      }
      onDone();
    } catch (e) {
      setError(formatErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const title =
    action === "publish"
      ? msg("grading.components.BatchActionDialog.congBoDiem")
      : action === "finalize"
        ? msg("grading.components.BatchActionDialog.chotDiem")
        : msg("grading.components.BatchActionDialog.deXuatAiHangLoat");
  const estimateIsCurrent = estimate.key === JSON.stringify(items);
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
          {result ? <Button onClick={onClose}>{msg("grading.components.BatchActionDialog.dong")}</Button> : <Button
            variant="primary"
            disabled={busy || items.length === 0}
            onClick={() => void run()}
          >
            {busy ? msg("grading.components.BatchActionDialog.dangXuLy") : msg("grading.components.BatchActionDialog.xacNhan")}
          </Button>}
        </>
      }
    >
      {/* Popup công bố điểm bỏ phần nội dung (yêu cầu 2026-11-07). */}
      {action !== "publish" && (
        <p className="font-display text-[34px] leading-none text-ink">{items.length} <span className="font-sans text-[13.5px] text-muted">{msg("grading.components.BatchActionDialog.baiDaChon")}</span></p>
      )}
      {/* Nguồn AI do hệ thống tự chọn: API chung nếu còn đủ hạn mức cho lượt này, không thì khóa cá nhân (yêu cầu 2026-11-05). */}
      {action === "ai" && (
        <p className="text-xs text-muted">
          {estimateState === "loading" ? msg("grading.components.BatchActionDialog.dangUocTinhToken") : estimateState === "ready" && estimate.tokens != null
            ? msg("grading.components.BatchActionDialog.uocTinhKhoangToken", { value: String(estimate.tokens.toLocaleString("vi-VN")) }) : msg("grading.components.BatchActionDialog.chuaUocTinhDuocToken")}
        </p>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {result && (
        <ul className="flex flex-col">
          {result.items.map((r, index) => {
            const detailMsg = r.code ? OUTCOME_CODE_MESSAGES[r.code] ?? msg("grading.components.BatchActionDialog.khongTheThucHienThaoTac") : undefined;
            return (
              <li key={`${r.evaluationId}-${index}`} className="border-b border-line py-2 last:border-b-0">
                <p>
                  {msg("grading.components.BatchActionDialog.bai")}{index + 1}: {r.outcome === "SUCCESS" ? msg("grading.components.BatchActionDialog.thanhCong") : r.outcome === "QUEUED" || r.outcome === "PENDING" ? msg("grading.components.BatchActionDialog.daGuiYeuCauAi") : r.outcome === "READY" ? msg("grading.components.BatchActionDialog.daCoDeXuatAi") : r.outcome === "SKIPPED" ? msg("grading.components.BatchActionDialog.boQua") : msg("grading.components.BatchActionDialog.chuaThucHienDuoc")}
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
