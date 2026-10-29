import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from "react";
import { cx } from "./cx";

/** Khung bảng bo 12px; dòng cuối không có viền dưới. */
export function TableFrame({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cx("overflow-x-auto rounded-xl border border-line bg-white", className)}>
      <table className="w-full border-collapse [&_tbody_tr:last-child>td]:border-b-0">{children}</table>
    </div>
  );
}

export function Th({ className, children, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      scope="col"
      className={cx(
        "border-b border-line bg-table-head px-3.5 py-2.5 text-left text-[11px] font-bold tracking-[.05em] whitespace-nowrap text-muted uppercase",
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
    <td className={cx("border-b border-line-soft px-3.5 py-[9px] text-left align-middle text-[13px] text-ink", className)} {...rest}>
      {children}
    </td>
  );
}

/** Dòng bấm được: đổi nền khi rê chuột. */
export function ClickableRow({ className, children, ...rest }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr className={cx("cursor-pointer hover:bg-canvas", className)} {...rest}>
      {children}
    </tr>
  );
}
