import type { ReactNode } from "react";
import { cx } from "./cx";
import { SOFT_TONES, type Tone } from "./tones";

/** Nhãn bo tròn chữ nhỏ (trạng thái, loại, số đếm). */
export function Pill({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span
      className={cx(
        "inline-flex h-[22px] items-center gap-1 rounded-full px-2.5 text-[11px] font-semibold whitespace-nowrap",
        SOFT_TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
