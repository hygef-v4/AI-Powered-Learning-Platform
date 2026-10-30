"use client";

// Module đang thu gọn, nhớ trong phiên (sessionStorage) để chuyển giữa các học liệu không mở lại hết. Trang Học liệu và
// sidebar module nhớ riêng: thu gọn ở một bên không làm đổi bên kia.

import { useCallback, useSyncExternalStore } from "react";

export type CollapseArea = "board" | "nav";

const EMPTY: ReadonlySet<string> = new Set();
const cache = new Map<CollapseArea, ReadonlySet<string>>();
const listeners = new Set<() => void>();
const storageKey = (area: CollapseArea) => `material-modules-collapsed:${area}`;

function read(area: CollapseArea): ReadonlySet<string> {
  const known = cache.get(area);
  if (known) return known;
  let value: ReadonlySet<string> = EMPTY;
  try {
    const raw = sessionStorage.getItem(storageKey(area));
    if (raw) value = new Set(JSON.parse(raw) as string[]);
  } catch { /* trình duyệt chặn bộ nhớ thì mở hết */ }
  cache.set(area, value);
  return value;
}

function write(area: CollapseArea, value: ReadonlySet<string>) {
  cache.set(area, value);
  try { sessionStorage.setItem(storageKey(area), JSON.stringify([...value])); } catch { /* chỉ nhớ trong trang */ }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}

/** Module nào đang thu gọn và hàm đóng / mở một module. */
export function useModuleCollapse(area: CollapseArea) {
  const collapsed = useSyncExternalStore(subscribe, () => read(area), () => EMPTY);
  const toggle = useCallback((moduleId: string) => {
    const next = new Set(read(area));
    if (next.has(moduleId)) next.delete(moduleId); else next.add(moduleId);
    write(area, next);
  }, [area]);
  return { collapsed, toggle };
}
