"use client";
import { useEffect, useRef, useState } from "react";
import { Alert, Button, Dialog } from "@/shared/ui";
import { Icon } from "@/shared/ui/Icon";
import { buildAiRequest } from "./AiProposalPanel";
import type { SourceChoice } from "@/shared/ai";
import { gradingApi } from "../api";
import { readGradingAiAccount, resumeGradingPolling } from "../model/gradingAiTasks";
import { formatErrorMessage } from "../model/format";
import type { BatchResult, ItemVersion } from "../model/types";

const OUTCOME_CODE_MESSAGES: Record<string, string> = {
  ALREADY_FINALIZED: "Bài này đã được chốt điểm trước đó. Điểm được giữ nguyên.",
  ALREADY_PUBLISHED: "Bài này đã được công bố điểm trước đó. Điểm được giữ nguyên.",
  INVALID_GRADE_STATE: "Trạng thái bài đã thay đổi hoặc chưa phù hợp với thao tác này. Tải lại danh sách để kiểm tra.",
  GRADE_VERSION_CONFLICT: "Bài chấm đã được cập nhật. Tải lại danh sách để dùng phiên bản mới nhất.",
  GRADE_LOCKED: "Điểm đã được khóa. Tải lại danh sách để xem trạng thái hiện tại.",
  GRADING_CLASS_READ_ONLY: "Lớp đã lưu trữ hoặc chưa mở. Không thể chỉnh sửa hay công bố điểm.",
  DUPLICATE_ITEM: "Bài bị chọn trùng; chỉ xử lý một lần.",
  INCOMPLETE_BATCH_RESULT: "Chưa nhận đủ kết quả xử lý. Tải lại danh sách để kiểm tra.",
  STRUCTURED_FEEDBACK_REVIEW_REQUIRED:
    "Còn mục nhận xét cần giảng viên kiểm tra và xác nhận trước khi công bố hoặc lưu điểm đã công bố.",
  STRUCTURED_FEEDBACK_SCORE_MISMATCH:
    "Có mục đã thay đổi điểm nhưng chưa cập nhật hoặc xác nhận lại giải thích nhận xét.",
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
  const [successMessage, setSuccessMessage] = useState("");
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
          ? action === "publish" ? `Đã công bố điểm cho ${changed} bài.` : `Đã chốt điểm cho ${changed} bài.`
          : "Các bài đã được chốt hoặc công bố trước đó. Điểm được giữ nguyên.";
        setSuccessMessage(message + (skipped > 0 && changed > 0 ? ` ${skipped} bài đã hoàn tất trước đó, điểm được giữ nguyên.` : ""));
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
      ? "Công bố điểm"
      : action === "finalize"
        ? "Chốt điểm"
        : "Đề xuất AI hàng loạt";
  const estimateIsCurrent = estimate.key === JSON.stringify(items);
  const estimateState = estimateIsCurrent ? estimate.state : "loading";
  if (successMessage) {
    return <SuccessNotice onDismiss={onClose} message={successMessage} />;
  }
  return (
    <Dialog
      open
      title={title}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          {result ? <Button onClick={onClose}>Đóng</Button> : <Button
            variant="primary"
            disabled={busy || items.length === 0}
            onClick={() => void run()}
          >
            {busy ? "Đang xử lý…" : "Xác nhận"}
          </Button>}
        </>
      }
    >
      {/* Popup công bố điểm bỏ phần nội dung (yêu cầu 2026-11-07). */}
      {action !== "publish" && (
        <p className="font-display text-[34px] leading-none text-ink">{items.length} <span className="font-sans text-[13.5px] text-muted">bài đã chọn</span></p>
      )}
      {/* Nguồn AI do hệ thống tự chọn: API chung nếu còn đủ hạn mức cho lượt này, không thì khóa cá nhân (yêu cầu 2026-11-05). */}
      {action === "ai" && (
        <p className="text-xs text-muted">
          {estimateState === "loading" ? "Đang ước tính token…" : estimateState === "ready" && estimate.tokens != null
            ? `Ước tính khoảng ${estimate.tokens.toLocaleString("vi-VN")} token.` : "Chưa ước tính được token."}
        </p>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {result && (
        <ul className="flex flex-col">
          {result.items.map((r, index) => {
            const detailMsg = r.code ? OUTCOME_CODE_MESSAGES[r.code] ?? "Không thể thực hiện thao tác. Tải lại danh sách và thử lại." : undefined;
            return (
              <li key={`${r.evaluationId}-${index}`} className="border-b border-line py-2 last:border-b-0">
                <p>
                  Bài {index + 1}: {r.outcome === "SUCCESS" ? "Thành công" : r.outcome === "QUEUED" || r.outcome === "PENDING" ? "Đã gửi yêu cầu AI" : r.outcome === "READY" ? "Đã có đề xuất AI" : r.outcome === "SKIPPED" ? "Bỏ qua" : "Chưa thực hiện được"}
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

/** Replaces the completed dialog without blocking the grading page or repeating the action. */
function SuccessNotice({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  const dismiss = useRef(onDismiss);
  useEffect(() => { dismiss.current = onDismiss; }, [onDismiss]);
  useEffect(() => {
    const timer = setTimeout(() => dismiss.current(), 4000);
    return () => clearTimeout(timer);
  }, []);
  return <div role="status" aria-live="polite" aria-atomic="true"
    className="fixed top-20 right-4 z-60 flex max-w-[calc(100vw-2rem)] items-center gap-2.5 rounded-xl border border-success/25 bg-success-soft px-4 py-3 text-sm font-medium text-success shadow-[var(--shadow-float)]">
    <Icon name="check_circle" size={20} />
    <span>{message}</span>
    <button type="button" aria-label="Đóng thông báo" onClick={onDismiss}
      className="ml-1 rounded-md p-1 hover:bg-success/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-success">
      <Icon name="close" size={18} />
    </button>
  </div>;
}
