import type { ButtonHTMLAttributes } from "react";
import { Icon, cx, type IconName } from "@/shared/ui";

const TONES = {
  neutral: "text-muted hover:bg-sunken hover:text-ink",
  primary: "text-muted hover:bg-primary-soft hover:text-primary-ink",
  danger: "text-danger hover:bg-danger-soft",
  success: "text-success hover:bg-success-soft",
};

/**
 * Nút icon nhỏ trên một hàng của sidebar (thêm mục, sửa, xóa học liệu…): cùng một hình, chỉ khác màu; ẩn khi thu gọn
 * sidebar. `title` mặc định là `label`.
 */
export function SidebarToolButton({ icon, label, tone = "neutral", iconSize = 14, className, ...rest }: {
  icon: IconName;
  label: string;
  tone?: keyof typeof TONES;
  iconSize?: number;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children">) {
  return (
    <button type="button" aria-label={label} title={label} {...rest}
      className={cx("flex size-6 shrink-0 items-center justify-center rounded-md transition-colors md:group-data-[collapsed]/sidebar:hidden",
        TONES[tone], className)}>
      <Icon name={icon} size={iconSize} />
    </button>
  );
}
