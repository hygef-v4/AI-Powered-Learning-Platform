"use client";
import { useEffect, useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { materialError } from "./api";

export function useMaterialResource<T>(url: string | null) {
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState<{ key: string; data?: T; error?: string }>({ key: "" });
  const key = `${url}:${revision}`;
  useEffect(() => {
    if (!url) return;
    const controller = new AbortController();
    apiFetch<T>(url, { signal: controller.signal }).then(
      (data) => { if (!controller.signal.aborted) setState({ key, data }); },
      (error) => { if (!controller.signal.aborted) setState({ key, error: materialError(error) }); },
    );
    return () => controller.abort();
  }, [url, key]);
  const current = state.key === key ? state : undefined;
  return { data: current?.data, error: current?.error, loading: !!url && !current,
    reload: () => setRevision((value) => value + 1) };
}
