"use client";
import { useCallback, useEffect, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { materialError } from "../api";

type ResourceOptions<T> = { pollWhile?: (data: T) => boolean; pollIntervalMs?: number; pollPaused?: boolean };

export function useMaterialResource<T>(url: string | null, { pollWhile, pollIntervalMs = 5000, pollPaused = false }: ResourceOptions<T> = {}) {
  const [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision((value) => value + 1), []);
  const [state, setState] = useState<{ key: string; data?: T; error?: string }>({ key: "" });
  const key = `${url}:${revision}`;
  useEffect(() => {
    if (!url) return;
    const controller = new AbortController();
    apiFetch<T>(url, { signal: controller.signal }).then(
      (data) => { if (!controller.signal.aborted) setState({ key, data }); },
      (error) => {
        if (!controller.signal.aborted) setState((previous) => ({
          key,
          ...(previous.key.startsWith(`${url}:`) && previous.data !== undefined ? { data: previous.data } : {}),
          error: materialError(error),
        }));
      },
    );
    return () => controller.abort();
  }, [url, key]);
  const sameResource = !!url && state.key.startsWith(`${url}:`);
  const data = sameResource ? state.data : undefined;
  const error = sameResource ? state.error : undefined;
  const settled = state.key === key;
  // Retain visible data during refresh, but schedule only after that request settles.
  useEffect(() => {
    if (!url || !settled || error || pollPaused || !pollWhile || data === undefined || !pollWhile(data)) return;
    const timer = setTimeout(reload, pollIntervalMs);
    return () => clearTimeout(timer);
  }, [url, settled, data, error, pollWhile, pollIntervalMs, pollPaused, reload]);
  return { data, error, loading: !!url && (!sameResource || (data === undefined && error === undefined)),
    reload };
}
