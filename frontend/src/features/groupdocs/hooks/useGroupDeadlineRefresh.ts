"use client";

import { useEffect } from "react";
import type { WorkspaceView } from "../model/types";

const DEADLINE_RETRY_MS = 10_000;
const MAX_TIMEOUT_MS = 2_147_483_647;

export function useGroupDeadlineRefresh(view: WorkspaceView | null, reload: (signal: AbortSignal) => Promise<void>) {
  const nextCutoff = view?.window === "ON_TIME" ? view.closesAt : view?.lateUntil ?? view?.closesAt;
  const deadline = view?.status === "IN_PROGRESS" ? nextCutoff : null;

  useEffect(() => {
    if (!deadline) return;
    const cutoff = Date.parse(deadline);
    if (!Number.isFinite(cutoff)) return;

    const controller = new AbortController();
    let timer: number;
    const scheduleRefresh = (delay: number) => {
      timer = window.setTimeout(readDeadline, Math.max(0, Math.min(delay, MAX_TIMEOUT_MS)));
    };
    const readDeadline = () => {
      const untilAfterCutoff = cutoff - Date.now() + 1;
      if (untilAfterCutoff > 0) {
        scheduleRefresh(untilAfterCutoff);
        return;
      }

      // SSE may miss closure, and the server can close the window before the scanner finalizes the document.
      void reload(controller.signal).then(() => {
        if (!controller.signal.aborted) scheduleRefresh(DEADLINE_RETRY_MS);
      });
    };

    scheduleRefresh(cutoff - Date.now() + 1);
    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [deadline, reload]);
}
