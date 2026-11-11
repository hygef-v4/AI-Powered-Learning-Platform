"use client";

import { useLayoutEffect, useRef, useState, type ReactNode } from "react";
import { cx } from "./cx";
import { handleTabListKey } from "./tabKeys";

export type TabItem<T extends string> = { key: T; label: ReactNode; count?: number };

/** Id của tab ứng với `key`, để vùng nội dung (`role="tabpanel"`, `id={panelId}`) đặt `aria-labelledby`. */
export function tabId(panelId: string, key: string) {
  return `${panelId}-tab-${key}`;
}

/**
 * Hàng tab gạch chân; gạch chân trượt sang tab vừa chọn, số đếm nhỏ cạnh nhãn. Có `panelId` thì tab đang chọn trỏ tới vùng
 * nội dung có id đó (`aria-controls`).
 */
export function Tabs<T extends string>({
  items,
  value,
  onChange,
  label,
  className,
  testIdPrefix,
  panelId,
}: {
  items: TabItem<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
  className?: string;
  testIdPrefix?: string;
  panelId?: string;
}) {
  const listRef = useRef<HTMLDivElement>(null);
  const [bar, setBar] = useState<{ left: number; width: number } | null>(null);
  useLayoutEffect(() => {
    const measure = () => {
      const active = listRef.current?.querySelector<HTMLElement>('[aria-selected="true"]');
      setBar(active ? { left: active.offsetLeft, width: active.offsetWidth } : null);
    };
    measure();
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(measure);
    if (observer && listRef.current) observer.observe(listRef.current);
    return () => observer?.disconnect();
  }, [value, items]);
  return (
    <div ref={listRef} role="tablist" aria-label={label} onKeyDown={handleTabListKey}
      className={cx("relative flex gap-6 overflow-x-auto border-b border-line [scrollbar-width:none]", className)}>
      {items.map((item) => {
        const on = item.key === value;
        return (
          <button
            key={item.key}
            type="button"
            role="tab"
            aria-selected={on}
            id={panelId ? tabId(panelId, item.key) : undefined}
            aria-controls={panelId && on ? panelId : undefined}
            tabIndex={on ? 0 : -1}
            onClick={() => onChange(item.key)}
            data-testid={testIdPrefix ? `${testIdPrefix}${item.key}` : undefined}
            className={cx(
              "flex cursor-pointer items-center gap-1.5 py-3 text-[13.5px] font-semibold whitespace-nowrap transition-colors duration-200",
              on ? "text-ink" : "text-muted hover:text-ink",
            )}
          >
            {item.label}
            {item.count !== undefined && <span className="font-mono text-xs font-medium text-muted">{item.count}</span>}
          </button>
        );
      })}
      {bar && (
        <span aria-hidden="true"
          className="pointer-events-none absolute bottom-0 left-0 h-0.5 rounded-full bg-primary transition-[transform,width] duration-300 ease-[cubic-bezier(.16,1,.3,1)]"
          style={{ width: bar.width, transform: `translateX(${bar.left}px)` }} />
      )}
    </div>
  );
}
