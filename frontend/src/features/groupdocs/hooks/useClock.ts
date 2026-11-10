"use client";

import { useEffect, useState } from "react";

/** Giờ hiện tại, cập nhật mỗi `intervalMs`; dùng cho nhắc hạn và "lưu lần cuối N phút trước". */
export function useClock(intervalMs = 30_000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(timer);
  }, [intervalMs]);
  return now;
}
