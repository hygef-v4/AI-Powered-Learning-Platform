"use client";
import { useEffect, useRef, useState } from "react";
import { Alert, Button, Dialog } from "@/shared/ui";
import { Icon } from "@/shared/ui/Icon";
import { aiRequest } from "./AiProposalPanel";
import { AiSourcePicker, type SourceChoice } from "@/shared/ai";
import { gradingApi } from "../api";
import { message } from "../hooks/useResource";
import type { BatchResult, ItemVersion } from "../model/types";

const OUTCOME_CODE_MESSAGES: Record<string, string> = {
  STRUCTURED_FEEDBACK_REVIEW_REQUIRED:
    "Còn mục nhận xét cấu trúc cần giảng viên kiểm tra và xác nhận trước khi chốt điểm.",
  STRUCTURED_FEEDBACK_SCORE_MISMATCH:
    "Có mục đã thay đổi điểm nhưng chưa cập nhật hoặc xác nhận lại giải thích nhận xét.",
};

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
  const [choosingSource, setChoosingSource] = useState(action === "ai");
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
    void gradingApi.estimateBatch(items).then(
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
    if (busy || items.length === 0 || (action === "ai" && !choice)) return;
    setBusy(true);
    setError("");
    try {
      let completed: BatchResult;
      if (action === "ai") {
        if (!choice) return;
        completed = await gradingApi.requestBatch(items, aiRequest(choice));
      } else {
        completed = await gradingApi[action](items);
      }
      setResult(completed);
      // Snapshot success before refreshing: a filtered list can remove the completed rows.
      if (action !== "ai" && completed.items.length === items.length
        && items.every((item) => completed.items.some((row) => row.evaluationId === item.evaluationId && row.outcome === "SUCCESS"))) {
        setSuccessMessage(action === "publish" ? `Đã công bố điểm cho ${items.length} bài.` : `Đã chốt điểm cho ${items.length} bài.`);
      }
      onDone();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
      setChoosingSource(false);
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
  if (action === "ai" && choosingSource && items.length > 0) {
    return <AiSourcePicker open
      task={`Đề xuất chấm ${items.length} bài đã chọn${estimateState === "loading" ? " · đang ước tính token" : estimateState === "failed" ? " · chưa ước tính được token" : ""}`}
      estimatedTokens={estimateState === "ready" ? estimate.tokens : undefined}
      busy={busy || estimateState === "loading"} onCancel={onClose} onSubmit={(choice) => void run(choice)} />;
  }
  return (
    <Dialog
      open
      title={title}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button
            variant="primary"
            disabled={busy || !!result || items.length === 0}
            onClick={() => action === "ai" ? setChoosingSource(true) : void run()}
          >
            {busy ? "Đang xử lý…" : "Xác nhận"}
          </Button>
        </>
      }
    >
      <p className="font-display text-[34px] leading-none text-ink">{items.length} <span className="font-sans text-[13.5px] text-muted">bài đã chọn</span></p>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {result && (
        <ul className="flex flex-col">
          {result.items.map((r, index) => {
            const detailMsg = r.code ? OUTCOME_CODE_MESSAGES[r.code] : undefined;
            return (
              <li key={`${r.evaluationId}-${index}`} className="border-b border-line py-2 last:border-b-0">
                <p>
                  Bài {index + 1}: {r.outcome}
                  {r.code && ` · ${r.code}`}
                </p>
                {detailMsg && (
                  <p role="alert" className="mt-1 text-xs font-medium text-danger">
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
