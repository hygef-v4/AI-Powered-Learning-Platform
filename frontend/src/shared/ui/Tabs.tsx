"use client";

import type { ReactNode } from "react";
import { cx } from "./cx";

export type TabItem<T extends string> = { key: T; label: ReactNode; count?: number };

/** Hàng tab gạch chân; số đếm nhỏ cạnh nhãn. */
export function Tabs<T extends string>({
  items,
  value,
  onChange,
  label,
  className,
}: {
  items: TabItem<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
  className?: string;
}) {
  return (
    <div role="tablist" aria-label={label} className={cx("flex gap-6 overflow-x-auto border-b border-line", className)}>
      {items.map((item) => {
        const on = item.key === value;
        return (
          <button
            key={item.key}
            type="button"
            role="tab"
            aria-selected={on}
            onClick={() => onChange(item.key)}
            className={cx(
              "-mb-px flex items-center gap-1.5 border-b-2 py-3 text-[13.5px] font-semibold whitespace-nowrap transition-colors duration-200",
              on ? "border-primary text-ink" : "border-transparent text-muted hover:text-ink",
            )}
          >
            {item.label}
            {item.count !== undefined && <span className="font-mono text-[11px] font-medium text-muted">{item.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
