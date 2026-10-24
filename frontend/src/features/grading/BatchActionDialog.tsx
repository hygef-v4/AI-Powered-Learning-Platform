"use client";
import { useEffect, useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { aiRequest } from "./AiProposalPanel";
import { AiSourcePicker, type SourceChoice } from "@/shared/ai";
import { gradingApi } from "./api";
import { message } from "./useResource";
import type { BatchResult, ItemVersion } from "./types";
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
      if (action === "ai") {
        if (!choice) return;
        setResult(await gradingApi.requestBatch(items, aiRequest(choice)));
      } else {
        setResult(await gradingApi[action](items));
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
          <button disabled={busy} onClick={onClose}>
            Đóng
          </button>
          <button
            disabled={busy || !!result || items.length === 0}
            onClick={() => action === "ai" ? setChoosingSource(true) : void run()}
            className="ml-3 rounded bg-primary px-4 py-2 text-white disabled:opacity-40"
          >
            {busy ? "Đang xử lý…" : "Xác nhận"}
          </button>
        </>
      }
    >
      <p>
        {items.length} bài đã chọn.{" "}
        {action === "publish"
          ? "Chỉ bài đã chốt được công bố. Sinh viên sẽ thấy điểm và nhận xét."
          : action === "finalize"
            ? "Chỉ bản nháp hợp lệ được chốt; sinh viên chưa thấy cho tới khi công bố."
            : "Mỗi bài nhận một đề xuất riêng; bạn kiểm tra và lưu từng bài."}
      </p>
      {error && <p role="alert">{error}</p>}
      {result && (
        <ul className="my-4 space-y-2">
          {result.items.map((r, index) => (
            <li key={`${r.evaluationId}-${index}`} className="rounded border p-2">
              Bài {index + 1}: {r.outcome}
              {r.code && ` · ${r.code}`}
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  );
}
