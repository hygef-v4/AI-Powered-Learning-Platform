"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { formatErrorMessage } from "../model/format";
export function useResource<T>(load: () => Promise<T>) {
  const [value, setValue] = useState<T | null>(null);
  const [error, setError] = useState("");
  const sequence = useRef(0);
  const refresh = useCallback(async () => {
    const current = ++sequence.current;
    setError("");
    try {
      const next = await load();
      if (sequence.current === current) setValue(next);
      return next;
    } catch (e) {
      if (sequence.current === current) setError(formatErrorMessage(e));
      throw e;
    }
  }, [load]);
  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (active) {
        setValue(null);
        void refresh().catch(() => {});
      }
    });
    return () => {
      active = false;
      sequence.current += 1;
    };
  }, [refresh]);
  return { value, error, refresh };
}
