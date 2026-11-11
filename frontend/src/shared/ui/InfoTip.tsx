"use client";

import { useId, useState } from "react";
import { Icon } from "./Icon";
import { cx } from "./cx";

/**
 * Chữ i nhỏ cạnh nhãn: rê chuột hoặc focus mới hiện lời giải thích. Rê chuột vào lời giải thích vẫn giữ nó (có dải nối
 * qua khe giữa nút và lời giải thích); Esc ẩn đi mà không mất focus, rời chuột hoặc focus thì lần sau lại hiện (WCAG 1.4.13).
 */
export function InfoTip({ text, label }: { text: string; label: string }) {
  const id = useId();
  const [dismissed, setDismissed] = useState(false);
  return (
    <span
      className="group relative inline-flex"
      onKeyDown={(event) => {
        if (event.key !== "Escape" || dismissed) return;
        event.stopPropagation();
        setDismissed(true);
      }}
      onMouseLeave={() => setDismissed(false)}
      onBlur={() => setDismissed(false)}
    >
      <button
        type="button"
        aria-label={`Giải thích: ${label}`}
        aria-describedby={id}
        className="inline-flex size-5 items-center justify-center rounded-full text-faint transition-colors hover:text-primary-ink focus-visible:text-primary-ink"
      >
        <Icon name="info" size={14} />
      </button>
      <span
        role="tooltip"
        id={id}
        className={cx(
          "invisible absolute bottom-full left-1/2 z-30 mb-2 w-max max-w-[280px] -translate-x-1/2 rounded-lg bg-ink px-3 py-2 text-xs leading-snug font-normal text-surface opacity-0 shadow-[var(--shadow-float)] transition-opacity duration-150",
          "after:absolute after:inset-x-0 after:top-full after:h-2 after:content-['']",
          !dismissed && "group-focus-within:visible group-focus-within:opacity-100 group-hover:visible group-hover:opacity-100",
        )}
      >
        {text}
      </span>
    </span>
  );
}
