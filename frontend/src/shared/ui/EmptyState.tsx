import type { ReactNode } from "react";
import { cx } from "./cx";

/** Trạng thái trống: vòng ensō mảnh, một dòng chữ có chân, dòng phụ và thao tác nếu có. */
export function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cx("flex animate-rise flex-col items-center gap-2 px-6 py-14 text-center", className)}>
      <svg viewBox="0 0 64 64" width="44" height="44" aria-hidden="true" className="mb-1 text-line-strong">
        <circle cx="32" cy="32" r="22" fill="none" stroke="currentColor" strokeWidth="3" strokeDasharray="110 30" strokeLinecap="round" />
      </svg>
      <p className="font-display text-[22px] leading-tight text-ink">{title}</p>
      {description && <p className="max-w-sm text-[13px] text-muted">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
