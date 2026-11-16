"use client";

import { useSyncExternalStore, type ReactNode } from "react";

const subscribe = () => () => {};

/**
 * Máy chủ dựng sẵn chữ của các client component bằng tiếng Việt. Khi người dùng chọn ngôn ngữ khác, nội dung trang chỉ
 * vẽ trên trình duyệt (sau khi nạp) để không lệch chữ giữa máy chủ và trình duyệt.
 */
export function LocaleGate({ deferred, children }: { deferred: boolean; children: ReactNode }) {
  const mounted = useSyncExternalStore(subscribe, () => true, () => false);
  return deferred && !mounted ? null : children;
}
