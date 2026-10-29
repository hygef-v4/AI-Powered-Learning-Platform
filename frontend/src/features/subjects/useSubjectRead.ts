"use client";

import { useCallback, useEffect, useState } from "react";

type Result<T> = { key: string; data: T; error?: never } | { key: string; error: unknown; data?: never };

/** Chỉ nhận response của request hiện tại; đổi key/retry che ngay dữ liệu cũ, không cache quyền. */
export function useSubjectRead<T>(key: string, load: () => Promise<T>, enabled = true) {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<Result<T> | null>(null);
  const requestKey = `${key}:${attempt}`;
  const reload = useCallback(() => setAttempt((value) => value + 1), []);

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    void Promise.resolve().then(load).then(
      (data) => { if (active) setResult({ key: requestKey, data }); },
      (error: unknown) => { if (active) setResult({ key: requestKey, error }); },
    );
    return () => { active = false; };
  }, [requestKey, load, enabled]);

  const current = enabled && result?.key === requestKey ? result : null;
  return { data: current?.data ?? null, error: current?.error ?? null, loading: enabled && !current, reload };
}
