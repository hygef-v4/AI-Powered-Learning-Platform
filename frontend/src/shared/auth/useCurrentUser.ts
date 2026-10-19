"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/shared/api/client";

export type CurrentUser = { displayName: string; email?: string };

let cached: Promise<CurrentUser | null> | null = null;

/** Xóa hồ sơ đã nhớ, gọi khi đăng xuất. */
export function clearCurrentUser(): void {
  cached = null;
}

/**
 * Hồ sơ của người đang đăng nhập (GET /api/v1/me/profile của U01); lỗi thì null, không chặn trang.
 * `enabled = false` khi nơi gọi đã có hồ sơ từ phiên U01 (không tải lần hai).
 */
export function useCurrentUser(enabled = true): CurrentUser | null {
  const [user, setUser] = useState<CurrentUser | null>(null);
  useEffect(() => {
    if (!enabled) return;
    let active = true;
    cached ??= apiFetch<CurrentUser>("/api/v1/me/profile").catch(() => {
      cached = null;
      return null;
    });
    void cached.then((loaded) => {
      if (active) setUser(loaded);
    });
    return () => {
      active = false;
    };
  }, [enabled]);
  return user;
}
