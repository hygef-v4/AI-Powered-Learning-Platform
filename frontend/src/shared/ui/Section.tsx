import type { ReactNode } from "react";
import { cx } from "./cx";
import type { IconName } from "./Icon";
import { IconTile } from "./IconTile";
import type { Tone } from "./tones";

type Props = {
  title?: ReactNode;
  description?: ReactNode;
  icon?: IconName;
  tone?: Tone;
  actions?: ReactNode;
  className?: string;
  children: ReactNode;
};

/** Thẻ nội dung nền trắng, bo 16px, có tiêu đề và mô tả. */
export function Section({ title, description, icon, tone = "primary", actions, className, children }: Props) {
  return (
    <section className={cx("flex min-w-0 flex-col gap-3.5 rounded-2xl border border-line bg-white p-5", className)}>
      {(title || actions) && (
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            {title && (
              <h2 className="flex items-center gap-2.5 text-[15px] font-bold text-ink">
                {icon && <IconTile icon={icon} tone={tone} size="sm" />}
                {title}
              </h2>
            )}
            {description && <p className="mt-[3px] text-xs text-muted">{description}</p>}
          </div>
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
