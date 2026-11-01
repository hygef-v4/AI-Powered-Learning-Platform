"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";

/** Lỗi tạm (mất mạng, 5xx, 429) thì poll tiếp; lỗi 4xx khác là trạng thái cuối (không tìm thấy, không có quyền). */
function transient(error: unknown): boolean {
  if (!(error instanceof ApiError)) return true;
  const status = error.problem.status;
  return status >= 500 || status === 429 || status === 0;
}

/**
 * Poll trạng thái việc nền của unit sở hữu mỗi 3 giây, dừng ở trạng thái cuối hoặc khi rời trang (NFR-U03-41). Một lần
 * mạng chập chờn không làm dừng hẳn: báo lỗi rồi thử lại, có kết quả thì xóa lỗi.
 */
export function usePollStatus<T>(fetcher: () => Promise<T>, isFinal: (value: T) => boolean, intervalMs = 3000) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    async function tick() {
      try {
        const value = await fetcher();
        if (stopped) return;
        setData(value);
        setError(null);
        if (!isFinal(value)) timer = setTimeout(tick, intervalMs);
      } catch (e) {
        if (stopped) return;
        setError(e);
        if (transient(e)) timer = setTimeout(tick, intervalMs);
      }
    }
    void tick();
    return () => {
      stopped = true;
      if (timer) clearTimeout(timer);
    };
    // fetcher và isFinal do nơi gọi giữ ổn định; chỉ chạy lại khi đổi chu kỳ.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [intervalMs]);

  return { data, error };
}
