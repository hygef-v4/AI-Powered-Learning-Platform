"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
export function message(error: unknown) {
  return error instanceof ApiError && error.problem.status === 409
    ? "Dữ liệu đã thay đổi. Tải lại để kiểm tra phiên bản mới; nội dung bạn đang sửa vẫn được giữ."
    : error instanceof Error
      ? error.message
      : "Không thể tải dữ liệu. Vui lòng thử lại.";
}
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
      if (sequence.current === current) setError(message(e));
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
