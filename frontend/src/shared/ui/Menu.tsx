"use client";

import { useEffect, useRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

type Props = {
  label: ReactNode;
  icon?: IconName;
  /** Nhãn đọc màn hình khi nút chỉ có ký hiệu. */
  ariaLabel?: string;
  align?: "left" | "right";
  /** {@code tool}: nút không viền trong thanh công cụ. */
  variant?: "button" | "tool";
  className?: string;
  summaryClassName?: string;
  children: ReactNode;
};

/** Nút mở danh sách thao tác (dựa trên {@code details}): bấm ra ngoài, Esc hoặc chọn một mục thì đóng. */
export function Menu({ label, icon, ariaLabel, align = "left", variant = "button", className, summaryClassName, children }: Props) {
  const ref = useRef<HTMLDetailsElement>(null);
  useEffect(() => {
    function close(event: Event) {
      const details = ref.current;
      if (!details?.open) return;
      if (event instanceof KeyboardEvent ? event.key === "Escape" : !details.contains(event.target as Node)) details.open = false;
    }
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, []);
  return (
    <details ref={ref} className={cx("relative", className)}
      onClick={(event) => {
        // Chọn một mục thì đóng menu.
        if ((event.target as HTMLElement).closest("[data-menu-item]") && ref.current) ref.current.open = false;
      }}>
      <summary aria-label={ariaLabel}
        className={cx("flex cursor-pointer list-none items-center gap-1.5 whitespace-nowrap select-none [&::-webkit-details-marker]:hidden",
          variant === "tool" ? "h-7 rounded-md px-2 text-[13px] font-medium text-body hover:bg-black/5"
            : "rounded-[9px] border border-line-strong bg-white px-3.5 py-2 text-[13px] font-semibold text-ink hover:bg-shell",
          summaryClassName)}>
        {icon && <Icon name={icon} size={variant === "tool" ? 18 : 17} />}
        {label}
        <Icon name="expand_more" size={16} className="text-muted" />
      </summary>
      <div role="menu" className={cx("absolute z-40 mt-1 flex min-w-48 flex-col rounded-xl border border-line bg-white p-1 shadow-lg",
        align === "right" ? "right-0" : "left-0")}>
        {children}
      </div>
    </details>
  );
}

export function MenuItem({ icon, tone, className, children, ...rest }: ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: IconName;
  tone?: "danger";
  children: ReactNode;
}) {
  return (
    <button type="button" role="menuitem" data-menu-item
      className={cx("flex items-center gap-2 rounded-lg px-3 py-2 text-left text-[13px] whitespace-nowrap hover:bg-shell disabled:opacity-45",
        tone === "danger" ? "text-danger" : "text-ink", className)} {...rest}>
      {icon && <Icon name={icon} size={17} className={tone === "danger" ? undefined : "text-muted"} />}
      {children}
    </button>
  );
}
