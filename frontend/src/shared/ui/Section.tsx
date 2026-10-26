import type { ReactNode } from "react";
import { cx } from "./cx";
import type { IconName } from "./Icon";
import type { Tone } from "./tones";

type Props = {
  title?: ReactNode;
  description?: ReactNode;
  /** Giữ để tương thích; khối nội dung tối giản không vẽ icon ở tiêu đề. */
  icon?: IconName;
  tone?: Tone;
  actions?: ReactNode;
  className?: string;
  children: ReactNode;
};

/** Khối nội dung phẳng: viền mảnh, bo 12px, tiêu đề nhỏ, thao tác bên phải. */
export function Section({ title, description, actions, className, children }: Props) {
  return (
    <section className={cx("flex min-w-0 animate-rise flex-col gap-4 rounded-xl border border-line bg-surface p-6", className)}>
      {(title || actions) && (
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="min-w-0">
            {title && <h2 className="flex items-center gap-2 text-[15px] font-bold text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-[12.5px] text-muted">{description}</p>}
          </div>
          {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
