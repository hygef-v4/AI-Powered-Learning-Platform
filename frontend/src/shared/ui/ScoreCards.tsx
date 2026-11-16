import type { ReactNode } from "react";
import { Card } from "./Card";
import { msg } from "@/i18n/msg";

/** Thanh tỉ lệ điểm đạt / điểm tối đa dưới các thẻ điểm. */
export function ScoreMeter({ awarded, max }: { awarded: number | null; max: number | null }) {
  const ratio = awarded != null && max ? Math.min(1, Math.max(0, awarded / max)) : 0;
  return (
    <span aria-hidden className="mt-2.5 block h-1 overflow-hidden rounded-full bg-sunken">
      <span className="block h-full rounded-full bg-primary" style={{ width: `${ratio * 100}%` }} />
    </span>
  );
}

/**
 * Thẻ Tổng điểm bên lề trang chấm bài và trang xem bài nộp: điểm lớn, điểm tối đa, thanh tỉ lệ; nội dung thêm
 * (nhận xét đã công bố…) đặt vào `children`.
 */
export function TotalScoreCard({
  score,
  maxScore,
  format,
  ariaLabel,
  children,
}: {
  score: number | null;
  maxScore: number | null;
  /** Cách viết điểm, ví dụ `formatPoints` hay `(n) => n.toFixed(2)`. */
  format: (value: number | null) => string;
  ariaLabel?: string;
  children?: ReactNode;
}) {
  return (
    <Card as="article" padding="none" lifted className="px-4 py-3.5">
      <p className="text-xs font-medium text-muted">{msg("shared.ui.ScoreCards.tongDiem")}</p>
      <p aria-label={ariaLabel} className="mt-1 flex items-baseline gap-1.5 tabular-nums">
        <span className="font-display text-[30px] leading-none tracking-[-0.02em] text-ink">{format(score)}</span>
        <span className="text-[13px] text-muted"> / {format(maxScore)}</span>
      </p>
      <ScoreMeter awarded={score} max={maxScore} />
      {children}
    </Card>
  );
}
