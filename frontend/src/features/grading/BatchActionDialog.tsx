"use client";
import { useState } from "react";
import { Dialog } from "@/shared/ui/Dialog";
import { AiSourceFields, aiRequest } from "./AiProposalPanel";
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
  const [source, setSource] = useState("");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<BatchResult | null>(null);
  const [error, setError] = useState("");
  async function run() {
    setBusy(true);
    setError("");
    try {
      setResult(
        action === "ai"
          ? await gradingApi.requestBatch(items, aiRequest(source))
          : await gradingApi[action](items),
      );
      onDone();
    } catch (e) {
      setError(message(e));
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
            disabled={busy || !!result || items.length === 0 || (action === "ai" && !source)}
            onClick={() => void run()}
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
      {action === "ai" && <AiSourceFields value={source} onChange={setSource} />}
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
