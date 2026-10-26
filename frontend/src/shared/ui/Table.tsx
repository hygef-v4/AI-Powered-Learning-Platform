import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from "react";
import { cx } from "./cx";

/** Bảng dạng tài liệu: không khung, chỉ đường kẻ mảnh giữa các dòng. */
export function TableFrame({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cx("overflow-x-auto", className)}>
      <table className="w-full border-collapse [&_tbody_tr:last-child>td]:border-b-0">{children}</table>
    </div>
  );
}

export function Th({ className, children, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      scope="col"
      className={cx(
        "border-b border-line px-3 pb-2.5 text-left text-[10.5px] font-semibold tracking-[.08em] whitespace-nowrap text-muted uppercase first:pl-1 last:pr-1",
        className,
      )}
      {...rest}
    >
      {children}
    </th>
  );
}

export function Td({ className, children, ...rest }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td className={cx("border-b border-line px-3 py-3 text-left align-middle text-[13.5px] text-ink first:pl-1 last:pr-1", className)} {...rest}>
      {children}
    </td>
  );
}

/** Dòng bấm được: nền nổi nhẹ khi rê chuột. */
export function ClickableRow({ className, children, ...rest }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr className={cx("cursor-pointer transition-colors duration-200 hover:bg-surface", className)} {...rest}>
      {children}
    </tr>
  );
}
