import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from "react";
import { cx } from "./cx";

const FILLED_HEAD =
  "[&_thead_th]:bg-sunken [&_thead_th]:pt-2.5 [&_thead_th]:text-body [&_thead_th]:border-line-strong " +
  "[&_th:first-child]:pl-3 [&_td:first-child]:pl-3 [&_th:last-child]:pr-3 [&_td:last-child]:pr-3";

type TableFrameProps = {
  children: ReactNode;
  className?: string;
  filledHead?: boolean;
  /** Màn hẹp (dưới `sm`): mỗi dòng thành một thẻ xếp dọc, ô có `label` hiện thành cặp nhãn và giá trị (`table-stack`). */
  stackOnMobile?: boolean;
};

/**
 * Bảng dạng tài liệu: không khung, chỉ đường kẻ mảnh giữa các dòng. {@code filledHead} tô nền hàng tiêu đề để tách
 * khỏi các dòng dữ liệu.
 */
export function TableFrame({ children, className, filledHead, stackOnMobile }: TableFrameProps) {
  return (
    <div className={cx("overflow-x-auto", filledHead && FILLED_HEAD, stackOnMobile && "table-stack", className)}>
      <table className="w-full border-collapse [&_tbody_tr:last-child>td]:border-b-0">{children}</table>
    </div>
  );
}

export function Th({ className, children, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      scope="col"
      className={cx(
        "border-b border-line px-3 pb-2.5 text-left text-xs font-semibold tracking-[.08em] whitespace-nowrap text-muted uppercase first:pl-1 last:pr-1",
        className,
      )}
      {...rest}
    >
      {children}
    </th>
  );
}

type TdProps = TdHTMLAttributes<HTMLTableCellElement> & {
  /** Tên cột, hiện trước giá trị khi bảng xếp thẻ trên màn hẹp. */
  label?: string;
  /** Ô chỉ để trang trí (mũi tên cuối dòng): ẩn khi bảng xếp thẻ. */
  hideWhenStacked?: boolean;
};

export function Td({ className, children, label, hideWhenStacked, ...rest }: TdProps) {
  return (
    <td data-label={label} data-stack-hidden={hideWhenStacked || undefined} className={cx("border-b border-line px-3 py-3 text-left align-middle text-[13.5px] text-ink first:pl-1 last:pr-1", className)} {...rest}>
      {children}
    </td>
  );
}

/** Dòng bấm được: rê chuột thì nền ấm lên, vạch màu chính hiện ở mép trái, mũi tên cuối dòng nhích sang phải (`row-interactive`). */
export function ClickableRow({ className, children, ...rest }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr className={cx("row-interactive cursor-pointer", className)} {...rest}>
      {children}
    </tr>
  );
}
