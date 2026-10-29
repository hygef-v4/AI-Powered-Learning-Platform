"use client";

import type { ReactNode } from "react";
import { cx, Icon, type IconName } from "@/shared/ui";
import { SOFT_TONES, type Tone } from "@/shared/ui/tones";

const BAND_TONES: Tone[] = ["primary", "info", "success", "accent", "orange"];

/** Màu dải trang trí cố định theo mã môn (cùng môn luôn cùng màu). */
function toneOf(seed: string): Tone {
  let hash = 0;
  for (const ch of seed) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return BAND_TONES[hash % BAND_TONES.length]!;
}

/**
 * Thẻ dọc (cao hơn rộng) của môn hoặc lớp trên Môn được phân công, Lớp được phân công, Lớp của tôi: phần trên là dải màu
 * trang trí in mờ mã môn dựng đứng bên trái, đọc từ trên xuống (dài thì chỉ thấy mấy chữ đầu), icon bên phải; phần dưới chỉ có mã (môn hoặc lớp) ở trên và tên ở dưới. Cả thẻ bấm được;
 * thẻ không mở được (lớp đã kết thúc của sinh viên) thì mờ đi.
 */
export function CourseCard({ code, name, band, icon, extra, onOpen, label, testId }: {
  code: string;
  name: string;
  /** Mã môn in mờ trên dải màu (quyết định màu dải). */
  band: string;
  icon: IconName;
  extra?: ReactNode;
  onOpen?: () => void;
  label?: string;
  testId?: string;
}) {
  const tone = toneOf(band);
  const body = <>
    <span aria-hidden className={cx("relative block min-h-0 flex-1 overflow-hidden", SOFT_TONES[tone])}>
      <span className="absolute top-3 -left-2 bottom-0 overflow-hidden font-display text-[64px] leading-none font-bold tracking-[-0.04em] whitespace-nowrap opacity-20 select-none [writing-mode:vertical-rl]">{band}</span>
      <span className="absolute -right-8 -bottom-10 size-32 rounded-full bg-surface/25" />
      <span className="absolute top-3.5 right-3.5 inline-flex size-9 items-center justify-center rounded-lg bg-surface/70"><Icon name={icon} size={18} /></span>
    </span>
    <span className="flex flex-col gap-0.5 px-4 pt-3 pb-4">
      <span className="flex items-center justify-between gap-2">
        <span className="truncate text-xs font-bold tracking-[.04em] text-muted">{code}</span>
        {extra}
      </span>
      <span className="flex items-end justify-between gap-2">
        <span className="line-clamp-2 min-h-[2.6em] text-[15px] leading-snug font-bold text-ink">{name}</span>
        {onOpen && (
          <Icon name="arrow_forward" size={18}
            className="mb-0.5 shrink-0 text-muted transition-[transform,color] duration-200 group-hover:translate-x-1 group-hover:text-primary-ink motion-reduce:transition-none" />
        )}
      </span>
    </span>
  </>;
  const frame = "flex aspect-[4/5] flex-col overflow-hidden rounded-xl border border-line bg-surface text-left";
  return onOpen ? (
    <button type="button" onClick={onOpen} aria-label={label} data-testid={testId}
      className={cx(frame, "group transition-[transform,box-shadow] duration-200 hover:-translate-y-0.5 hover:shadow-[var(--shadow-lift)] focus-visible:-translate-y-0.5")}>
      {body}
    </button>
  ) : <div className={cx(frame, "opacity-70")} data-testid={testId}>{body}</div>;
}

/** Lưới thẻ dọc chung cho ba trang danh sách. */
export function CourseGrid({ children, empty }: { children: ReactNode; empty?: string | null }) {
  return empty
    ? <p className="rounded-xl border border-dashed border-line-strong bg-surface/60 px-6 py-12 text-center text-[13.5px] text-muted">{empty}</p>
    : <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">{children}</div>;
}
