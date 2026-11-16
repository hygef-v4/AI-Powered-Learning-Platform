import type { ComponentPropsWithoutRef, ElementType } from "react";
import { cx } from "./cx";

type Padding = "none" | "sm" | "md" | "lg";

const PADDING: Record<Padding, string> = {
  none: "",
  sm: "p-3",
  md: "p-4",
  lg: "p-5",
};

type Props<T extends ElementType> = {
  /** Thẻ HTML bọc ngoài; mặc định `div`, dùng `section`/`article`/`li` khi cần ngữ nghĩa. */
  as?: T;
  /** Khoảng đệm trong thẻ: none (tự đệm), sm 12px, md 16px (mặc định), lg 20px. */
  padding?: Padding;
  /** Bóng nổi nhẹ cho thẻ cần tách khỏi nền (thanh dính, khối nổi). */
  lifted?: boolean;
} & Omit<ComponentPropsWithoutRef<T>, "as" | "padding" | "lifted">;

/**
 * Thẻ nội dung chuẩn: bo 12px, viền mảnh, nền surface. Mọi khối dạng card trong features dùng chung component này
 * thay vì tự lặp lại bộ class; tiêu đề + thao tác dùng `Section`, số liệu dùng `StatTile`.
 */
export function Card<T extends ElementType = "div">({ as, padding = "md", lifted, className, children, ...rest }: Props<T>) {
  const Tag: ElementType = as ?? "div";
  return (
    <Tag className={cx("rounded-xl border border-line bg-surface", PADDING[padding], lifted && "shadow-[var(--shadow-lift)]", className)} {...rest}>
      {children}
    </Tag>
  );
}
