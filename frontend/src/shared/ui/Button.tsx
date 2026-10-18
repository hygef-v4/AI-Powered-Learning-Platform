import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "md" | "sm";

const VARIANTS: Record<Variant, string> = {
  primary: "border-primary bg-primary text-white hover:border-primary-strong hover:bg-primary-strong",
  secondary: "border-line-strong bg-white text-ink hover:bg-shell",
  danger: "border-danger bg-danger text-white hover:opacity-90",
  ghost: "border-transparent bg-transparent text-body hover:bg-shell hover:text-ink",
};

const SIZES: Record<Size, { box: string; icon: number }> = {
  md: { box: "gap-1.5 px-3.5 py-2 text-[13px]", icon: 17 },
  sm: { box: "gap-1.5 px-2.5 py-[5px] text-xs", icon: 16 },
};

const BASE =
  "inline-flex items-center justify-center rounded-[9px] border font-semibold whitespace-nowrap transition-colors " +
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary " +
  "disabled:cursor-not-allowed disabled:opacity-45";

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
