"use client";

import { useCallback, useState } from "react";
import { fetchApi } from "@/shared/api/client";
import { usePollStatus } from "@/shared/status/usePollStatus";
import { Button, Pill, type Tone } from "@/shared/ui";
import type { AiTaskState, AiTaskView } from "./types";
import { msg } from "@/i18n/msg";

const FINAL: AiTaskState[] = ["READY", "FAILED", "UNKNOWN", "CANCELLED"];

const LABEL: Record<AiTaskState, { text: string; tone: Tone }> = {
  QUEUED: { text: msg("shared.ai.AiTaskStatus.pending"), tone: "neutral" },
  RUNNING: { text: msg("shared.ai.AiTaskStatus.running"), tone: "info" },
  READY: { text: msg("shared.ai.AiTaskStatus.done"), tone: "success" },
  FAILED: { text: msg("shared.ai.AiTaskStatus.error"), tone: "danger" },
  UNKNOWN: { text: msg("shared.ai.AiTaskStatus.unknownResult"), tone: "warning" },
  CANCELLED: { text: msg("shared.ai.AiTaskStatus.cancelled"), tone: "neutral" },
};

type Props = {
  taskId: string;
  cancellable?: boolean;
  intervalMs?: number;
  /** Gọi một lần khi task tới trạng thái cuối. */
  onFinished?: (task: AiTaskView) => void;
};

/** Theo dõi task AI mỗi 2 giây tới khi kết thúc; còn chờ thì cho hủy (BR-U06-26). */
export function AiTaskStatus({ taskId, cancellable = true, intervalMs = 2000, onFinished }: Props) {
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const fetcher = useCallback(() => fetchApi<AiTaskView>(`/api/v1/ai/tasks/${encodeURIComponent(taskId)}`), [taskId]);
  const isFinal = useCallback(
    (task: AiTaskView) => {
      const done = FINAL.includes(task.state);
      if (done) onFinished?.(task);
      return done;
    },
    [onFinished],
  );
  const { data, error } = usePollStatus(fetcher, isFinal, intervalMs);

  const cancel = async () => {
    setCancelling(true);
    setCancelError(null);
    try {
      await fetchApi<void>(`/api/v1/ai/tasks/${encodeURIComponent(taskId)}:cancel`, { method: "POST" });
    } catch {
      setCancelError(msg("shared.ai.AiTaskStatus.couldNotCancelTheTask"));
    } finally {
      setCancelling(false);
    }
  };

  if (error && !data) return <p className="text-xs text-danger">{msg("shared.ai.AiTaskStatus.couldNotViewTheAi")}</p>;
  if (!data) return <p className="text-xs text-muted">{msg("shared.ai.AiTaskStatus.loadingStatus")}</p>;
  const label = LABEL[data.state];
  return (
    <div className="flex flex-wrap items-center gap-2 text-xs" aria-live="polite">
      <Pill tone={label.tone}>{label.text}</Pill>
      {data.state === "UNKNOWN" && (
        <span className="text-muted">{msg("shared.ai.AiTaskStatus.theProviderDidNotReturn")}</span>
      )}
      {data.safeError && data.state !== "UNKNOWN" && <span className="text-danger">{data.safeError}</span>}
      {data.applyResult === "STALE" && <span className="text-warning">{msg("shared.ai.AiTaskStatus.theDataChangedWhileWaiting")}</span>}
      {cancellable && data.state === "QUEUED" && (
        <Button size="sm" onClick={() => void cancel()} disabled={cancelling}>
          {msg("shared.ai.AiTaskStatus.cancel")}</Button>
      )}
      {cancelError && <span className="text-danger">{cancelError}</span>}
    </div>
  );
}
