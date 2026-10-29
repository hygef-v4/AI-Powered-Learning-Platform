import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "md" | "sm";

const VARIANTS: Record<Variant, string> = {
  primary: "border-primary bg-primary text-on-primary hover:border-primary-strong hover:bg-primary-strong",
  secondary: "border-line-strong bg-surface text-ink hover:bg-sunken",
  danger: "border-danger bg-danger text-on-danger hover:opacity-90",
  ghost: "border-transparent bg-transparent text-body hover:bg-sunken hover:text-ink",
};

const SIZES: Record<Size, { box: string; icon: number }> = {
  md: { box: "h-9 gap-2 px-3.5 text-[13px]", icon: 16 },
  sm: { box: "h-8 gap-1.5 px-2.5 text-xs", icon: 14 },
};

const BASE =
  "inline-flex items-center justify-center rounded-lg border font-semibold whitespace-nowrap select-none " +
  "transition-[background-color,border-color,color,transform,opacity] duration-200 active:scale-[.98] " +
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus " +
  "disabled:cursor-not-allowed disabled:opacity-45 disabled:active:scale-100";

type Common = { variant?: Variant; size?: Size; icon?: IconName; children: ReactNode };

export function Button({
  variant = "secondary",
  size = "md",
  icon,
  className,
  type = "button",
  children,
  ...rest
}: Common & ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button type={type} className={cx(BASE, VARIANTS[variant], SIZES[size].box, className)} {...rest}>
      {icon && <Icon name={icon} size={SIZES[size].icon} />}
      {children}
    </button>
  );
}

export function ButtonLink({
  href,
  variant = "secondary",
  size = "md",
  icon,
  className,
  children,
}: Common & { href: string; className?: string }) {
  return (
    <Link href={href} className={cx(BASE, VARIANTS[variant], SIZES[size].box, className)}>
      {icon && <Icon name={icon} size={SIZES[size].icon} />}
      {children}
    </Link>
  );
}

/** Nút chỉ có icon (đóng, xóa dòng…); bắt buộc có nhãn đọc màn hình. */
export function IconButton({
  icon,
  label,
  size = 32,
  className,
  type = "button",
  ...rest
}: { icon: IconName; label: string; size?: number } & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children">) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      style={{ width: size, height: size }}
      className={cx(
        "inline-flex shrink-0 items-center justify-center rounded-lg text-muted transition-colors duration-200",
        "hover:bg-sunken hover:text-ink active:scale-95 disabled:cursor-not-allowed disabled:opacity-45",
        className,
      )}
      {...rest}
    >
      <Icon name={icon} size={Math.round(size / 2)} />
    </button>
  );
}
