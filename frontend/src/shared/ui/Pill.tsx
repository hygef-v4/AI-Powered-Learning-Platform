import type { ReactNode } from "react";
import { cx } from "./cx";
import { SOFT_TONES, type Tone } from "./tones";

/** Nhãn bo tròn (trạng thái, số đếm, loại). */
export function Pill({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span
      className={cx(
        "inline-flex items-center gap-1 rounded-full px-[9px] py-0.5 text-[11px] font-semibold whitespace-nowrap",
        SOFT_TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
