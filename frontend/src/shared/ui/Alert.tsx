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

/** Dải thông báo nền màu nhạt: cảnh báo, lỗi, thông tin. */
export function Alert({ tone = "info", title, action, role, className, children }: Props) {
  return (
    <div role={role} className={cx("flex animate-fade items-start gap-2.5 rounded-lg px-3.5 py-2.5 text-[13px] leading-normal", TONES[tone].box, className)}>
      <Icon name={TONES[tone].icon} size={16} className="mt-0.5" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-semibold">{title}</p>}
        {children}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
