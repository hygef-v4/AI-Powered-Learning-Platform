"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";

export type PaletteItem = { href: string; label: string; icon?: IconName; group?: string };

/** Bỏ dấu tiếng Việt để gõ "bai tap" vẫn ra "Bài tập". */
function fold(text: string) {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase();
}

/** Tìm nhanh (Ctrl K): gõ để lọc các trang của vai trò, Enter để mở. */
export function CommandPalette({ open, items, onClose }: { open: boolean; items: PaletteItem[]; onClose: () => void }) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const input = useRef<HTMLInputElement>(null);

  const results = useMemo(() => {
    const q = fold(query.trim());
    return q ? items.filter((item) => fold(`${item.label} ${item.group ?? ""}`).includes(q)) : items;
  }, [items, query]);

  useEffect(() => {
    if (!open) return;
    input.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  function go(item: PaletteItem | undefined) {
    if (!item) return;
    onClose();
    router.push(item.href);
  }

  return (
    <div
      className="fixed inset-0 z-50 flex animate-fade justify-center bg-[var(--c-scrim)] px-4 pt-[14vh] backdrop-blur-[2px]"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div role="dialog" aria-modal="true" aria-label="Tìm nhanh" className="flex h-fit max-h-[60vh] w-[560px] max-w-full animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-[var(--shadow-float)]">
        <div className="flex items-center gap-3 border-b border-line px-4">
          <Icon name="search" size={16} className="text-muted" />
          <input
            ref={input}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setActive(0);
            }}
            onKeyDown={(event) => {
              if (event.key === "ArrowDown") {
                event.preventDefault();
                setActive((value) => Math.min(value + 1, results.length - 1));
              } else if (event.key === "ArrowUp") {
                event.preventDefault();
                setActive((value) => Math.max(value - 1, 0));
              } else if (event.key === "Enter") {
                go(results[active]);
              }
            }}
            placeholder="Đi tới trang…"
            aria-label="Đi tới trang"
            className="h-14 flex-1 bg-transparent text-[15px] text-ink outline-none placeholder:text-faint"
          />
          <kbd className="rounded border border-line-strong bg-sunken px-1.5 font-mono text-[10.5px] text-muted">Esc</kbd>
        </div>
        <ul role="listbox" aria-label="Kết quả" className="overflow-y-auto p-1.5">
          {results.map((item, index) => (
            <li key={item.href} role="option" aria-selected={index === active}>
              <button
                type="button"
                onMouseEnter={() => setActive(index)}
                onClick={() => go(item)}
                className={cx(
                  "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-[13.5px] transition-colors",
                  index === active ? "bg-primary-soft text-ink" : "text-body",
                )}
              >
                {item.icon && <Icon name={item.icon} size={16} className="text-muted" />}
                <span className="flex-1 font-semibold">{item.label}</span>
                {item.group && <span className="text-xs text-muted">{item.group}</span>}
              </button>
            </li>
          ))}
          {results.length === 0 && <li className="px-3 py-6 text-center text-[13px] text-muted">Không có trang khớp</li>}
        </ul>
      </div>
    </div>
  );
}
