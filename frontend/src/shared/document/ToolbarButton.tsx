import type { ButtonHTMLAttributes } from "react";
import { Icon, cx, type IconName } from "@/shared/ui";

/** Nút trên thanh công cụ của trang soạn; giữ con trỏ trong ô đang gõ khi bấm. */
export function ToolbarButton({ icon, active, className, children, ...rest }: ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: IconName;
  active?: boolean;
}) {
  return (
    <button type="button" aria-pressed={active} onMouseDown={(event) => event.preventDefault()}
      className={cx("inline-flex h-7 min-w-7 items-center justify-center gap-1 rounded-md px-1.5 text-[13px] font-medium whitespace-nowrap text-body transition-colors enabled:cursor-pointer enabled:hover:bg-line-strong enabled:focus-visible:bg-line-strong disabled:cursor-default disabled:opacity-35",
        active && "bg-primary-soft text-ink", className)}
      {...rest}>
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  );
}
