import type { ReactNode } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

type AlertTone = "warning" | "danger" | "info" | "success";

const TONES: Record<AlertTone, { box: string; icon: IconName }> = {
  warning: { box: "bg-warning-soft text-warning", icon: "warning" },
  danger: { box: "bg-danger-soft text-danger", icon: "error" },
  info: { box: "bg-info-soft text-info", icon: "info" },
  success: { box: "bg-success-soft text-success", icon: "check_circle" },
};

type Props = {
  tone?: AlertTone;
  title?: ReactNode;
  /** Nút bên phải, ví dụ Tải lại. */
  action?: ReactNode;
  /** "alert" khi cần trình đọc màn hình đọc ngay (lỗi, xung đột). */
  role?: "alert" | "status";
  className?: string;
  children?: ReactNode;
};

/** Hộp thông báo nền màu: cảnh báo, lỗi, thông tin. */
export function Alert({ tone = "info", title, action, role, className, children }: Props) {
  return (
    <div
      role={role}
      className={cx("flex items-start gap-2.5 rounded-[11px] px-3.5 py-[11px] text-[12.5px] leading-normal", TONES[tone].box, className)}
    >
      <Icon name={TONES[tone].icon} size={18} className="mt-px" />
      <div className="min-w-0 flex-1">
        {title && <p className="mb-0.5 font-bold">{title}</p>}
        {children}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
