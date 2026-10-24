"use client";
import { useEffect, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { materialError } from "./api";

type ResourceOptions<T> = { pollWhile?: (data: T) => boolean; pollIntervalMs?: number };

export function useMaterialResource<T>(url: string | null, { pollWhile, pollIntervalMs = 3000 }: ResourceOptions<T> = {}) {
  const [revision, setRevision] = useState(0);
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
  useEffect(() => {
    if (!url || !pollWhile || data === undefined || !pollWhile(data)) return;
    const timer = setTimeout(() => setRevision((value) => value + 1), pollIntervalMs);
    return () => clearTimeout(timer);
  }, [url, data, pollWhile, pollIntervalMs]);
  return { data, error, loading: !!url && (!sameResource || (data === undefined && error === undefined)),
    reload: () => setRevision((value) => value + 1) };
}
