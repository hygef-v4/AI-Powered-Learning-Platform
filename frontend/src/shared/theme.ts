"use client";

import { useCallback, useSyncExternalStore } from "react";
import { DARK_SCHEME_QUERY, THEME_KEY as KEY } from "./themeBoot";

export type Theme = "light" | "dark";

function readCurrentTheme(): Theme {
  return document.documentElement.dataset.theme === "dark" ? "dark" : "light";
}

/** Lựa chọn người dùng đã lưu; chưa chọn (hoặc trình duyệt chặn lưu trữ) thì `null`. */
function loadStoredTheme(): Theme | null {
  try {
    const stored = localStorage.getItem(KEY);
    return stored === "dark" || stored === "light" ? stored : null;
  } catch {
    return null;
  }
}

const listeners = new Set<() => void>();
/** MediaQueryList đã gắn listener; gỡ đúng trên đối tượng này, vì mỗi lần gọi `matchMedia` trả một đối tượng mới. */
let systemQuery: MediaQueryList | null = null;

function applyTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme;
  listeners.forEach((listener) => listener());
}

/** Chưa chọn theme thì đi theo hệ điều hành, kể cả khi hệ điều hành đổi sáng/tối lúc trang đang mở. */
function followSystemTheme(event: MediaQueryListEvent) {
  if (loadStoredTheme() === null) applyTheme(event.matches ? "dark" : "light");
}

function subscribe(listener: () => void) {
  if (listeners.size === 0 && typeof window.matchMedia === "function") {
    systemQuery = window.matchMedia(DARK_SCHEME_QUERY);
    systemQuery.addEventListener?.("change", followSystemTheme);
  }
  listeners.add(listener);
  return () => {
    if (!listeners.delete(listener) || listeners.size > 0) return;
    systemQuery?.removeEventListener?.("change", followSystemTheme);
    systemQuery = null;
  };
}

export function setTheme(theme: Theme) {
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    // Trình duyệt chặn lưu trữ: theme vẫn đổi cho phiên hiện tại.
  }
  applyTheme(theme);
}

/** Theme đang dùng và hàm đảo sáng/tối. */
export function useTheme(): [Theme, () => void] {
  const theme = useSyncExternalStore(subscribe, readCurrentTheme, () => "light" as Theme);
  const toggle = useCallback(() => setTheme(readCurrentTheme() === "dark" ? "light" : "dark"), []);
  return [theme, toggle];
}
