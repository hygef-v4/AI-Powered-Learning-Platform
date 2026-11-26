"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { formatErrorMessage } from "../model/format";
// Quản lý dữ liệu và lỗi của một nguồn, bỏ kết quả thuộc lần tải đã cũ.
export function useResource<T>(load: () => Promise<T>) {
  const [value, setValue] = useState<T | null>(null);
  const [error, setError] = useState("");
  const sequence = useRef(0);
  // Đọc lại nguồn và chỉ cập nhật UI nếu số thứ tự vẫn là lần tải mới nhất.
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
