"use client";

import { useCallback, useSyncExternalStore } from "react";
import { THEME_KEY as KEY } from "./themeBoot";

export type Theme = "light" | "dark";

function current(): Theme {
  return document.documentElement.dataset.theme === "dark" ? "dark" : "light";
}

const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function setTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme;
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    // Trình duyệt chặn lưu trữ: theme vẫn đổi cho phiên hiện tại.
  }
  listeners.forEach((listener) => listener());
}

/** Theme đang dùng và hàm đảo sáng/tối. */
export function useTheme(): [Theme, () => void] {
  const theme = useSyncExternalStore(subscribe, current, () => "light" as Theme);
  const toggle = useCallback(() => setTheme(current() === "dark" ? "light" : "dark"), []);
  return [theme, toggle];
}
