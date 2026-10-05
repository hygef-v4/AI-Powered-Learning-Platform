"use client";

import { useEffect, useRef, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api/client";

export type PollState<T> = {
  data: T | undefined;
  error: ApiError | undefined;
  polling: boolean;
};

type Options<T> = {
  /** Trạng thái cuối do unit sở hữu khai báo, ví dụ `INDEXED`, `FAILED`. */
  isFinal: (data: T) => boolean;
  intervalMs?: number;
  /** `false` thì chưa poll (ví dụ chưa có id). */
  enabled?: boolean;
};

/**
 * Poll API trạng thái của unit sở hữu mỗi 3 giây (NFR-U03-41); dừng ở trạng thái cuối, khi lỗi, hoặc khi rời trang.
 * U03 không có API trạng thái việc nền riêng (BR-U03-61).
 */
export function usePollStatus<T>(path: string, { isFinal, intervalMs = 3000, enabled = true }: Options<T>): PollState<T> {
  const [state, setState] = useState<PollState<T>>({ data: undefined, error: undefined, polling: enabled });
  const isFinalRef = useRef(isFinal);
  isFinalRef.current = isFinal;

  useEffect(() => {
    if (!enabled) {
      setState((s) => ({ ...s, polling: false }));
      return;
    }
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const controller = new AbortController();

    const tick = async () => {
      try {
        const data = await apiFetch<T>(path, { signal: controller.signal });
        if (cancelled) return;
        const done = isFinalRef.current(data);
        setState({ data, error: undefined, polling: !done });
        if (!done) timer = setTimeout(tick, intervalMs);
      } catch (e) {
        if (cancelled) return;
        setState((s) => ({ ...s, error: e instanceof ApiError ? e : undefined, polling: false }));
      }
    };

    setState((s) => ({ ...s, polling: true }));
    void tick();
    return () => {
      cancelled = true;
      controller.abort();
      if (timer) clearTimeout(timer);
    };
  }, [path, intervalMs, enabled]);

  return state;
}
