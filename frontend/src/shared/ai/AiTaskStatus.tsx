"use client";

import { useCallback, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { usePollStatus } from "@/shared/status/usePollStatus";
import { Button, Pill, type Tone } from "@/shared/ui";
import type { AiTaskState, AiTaskView } from "./types";

const FINAL: AiTaskState[] = ["READY", "FAILED", "UNKNOWN", "CANCELLED"];

const LABEL: Record<AiTaskState, { text: string; tone: Tone }> = {
  QUEUED: { text: "Đang chờ", tone: "neutral" },
  RUNNING: { text: "Đang chạy", tone: "info" },
  READY: { text: "Xong", tone: "success" },
  FAILED: { text: "Lỗi", tone: "danger" },
  UNKNOWN: { text: "Không rõ kết quả", tone: "warning" },
  CANCELLED: { text: "Đã hủy", tone: "neutral" },
};

type Props = {
  taskId: string;
  /** Gọi một lần khi task tới trạng thái cuối. */
  onFinished?: (task: AiTaskView) => void;
};

/** Theo dõi task AI mỗi 2 giây tới khi kết thúc; còn chờ thì cho hủy (BR-U06-26). */
export function AiTaskStatus({ taskId, onFinished }: Props) {
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const fetcher = useCallback(() => apiFetch<AiTaskView>(`/api/v1/ai/tasks/${encodeURIComponent(taskId)}`), [taskId]);
  const isFinal = useCallback(
    (task: AiTaskView) => {
      const done = FINAL.includes(task.state);
      if (done) onFinished?.(task);
      return done;
    },
    [onFinished],
  );
  const { data, error } = usePollStatus(fetcher, isFinal, 2000);

  const cancel = async () => {
    setCancelling(true);
    setCancelError(null);
    try {
      await apiFetch<void>(`/api/v1/ai/tasks/${encodeURIComponent(taskId)}:cancel`, { method: "POST" });
    } catch {
      setCancelError("Không hủy được, task có thể đã bắt đầu chạy.");
    } finally {
      setCancelling(false);
    }
  };

  if (error && !data) return <p className="text-xs text-danger">Không xem được trạng thái yêu cầu AI.</p>;
  if (!data) return <p className="text-xs text-muted">Đang tải trạng thái...</p>;
  const label = LABEL[data.state];
  return (
    <div className="flex flex-wrap items-center gap-2 text-xs" aria-live="polite">
      <Pill tone={label.tone}>{label.text}</Pill>
      {data.state === "UNKNOWN" && (
        <span className="text-muted">Nhà cung cấp không trả kết quả rõ ràng; kiểm tra lại trước khi gửi lần nữa.</span>
      )}
      {data.safeError && data.state !== "UNKNOWN" && <span className="text-danger">{data.safeError}</span>}
      {data.applyResult === "STALE" && <span className="text-warning">Dữ liệu đã đổi trong lúc chờ, kết quả không được áp dụng.</span>}
      {data.state === "QUEUED" && (
        <Button size="sm" onClick={() => void cancel()} disabled={cancelling}>
          Hủy
        </Button>
      )}
      {cancelError && <span className="text-danger">{cancelError}</span>}
    </div>
  );
}
