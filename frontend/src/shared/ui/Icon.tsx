import { cx } from "./cx";
import { ICON_PATHS, type IconName } from "./icon-paths";

export type { IconName };

type Props = {
  name: IconName;
  size?: number;
  filled?: boolean;
  className?: string;
  /** Có title thì icon được đọc như ảnh; không có thì ẩn với trình đọc màn hình. */
  title?: string;
};

/** Icon Phosphor (Bold, Fill khi đang chọn) dạng SVG, màu theo currentColor. */
export function Icon({ name, size = 18, filled = false, className, title }: Props) {
  const paths = ICON_PATHS[name];
  return (
    <svg
      viewBox="0 0 256 256"
      width={size}
      height={size}
      fill="currentColor"
      focusable="false"
      className={cx("shrink-0", className)}
      role={title ? "img" : undefined}
      aria-hidden={title ? undefined : true}
    >
      {title && <title>{title}</title>}
      <path d={filled ? paths.fill : paths.outline} />
    </svg>
  );
}
