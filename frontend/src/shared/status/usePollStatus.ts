"use client";

import { useEffect, useState } from "react";

/** Poll trạng thái việc nền của unit sở hữu mỗi 3 giây, dừng ở trạng thái cuối hoặc khi rời trang (NFR-U03-41). */
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
        if (!isFinal(value)) timer = setTimeout(tick, intervalMs);
      } catch (e) {
        if (!stopped) setError(e);
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
