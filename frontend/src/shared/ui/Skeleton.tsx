import { cx } from "./cx";

/** Khối xám giữ chỗ cho nội dung đang tải; chỉ là hình, trình đọc màn hình bỏ qua. */
export function Skeleton({ className }: { className?: string }) {
  return <span aria-hidden className={cx("block h-3.5 animate-pulse rounded bg-sunken motion-reduce:animate-none", className)} />;
}

const WIDTHS = ["w-3/4", "w-1/2", "w-2/3", "w-2/5"];

/**
 * Các dòng giữ chỗ của một bảng đang tải: đủ số cột để bảng không nhảy bố cục khi dữ liệu về. Dòng đầu mang nhãn cho
 * trình đọc màn hình.
 */
export function TableSkeletonRows({ rows = 5, columns, label = "Đang tải…" }: { rows?: number; columns: number; label?: string }) {
  return (
    <>
      {Array.from({ length: rows }, (_, row) => (
        <tr key={row}>
          {Array.from({ length: columns }, (_, column) => (
            <td key={column} className="border-b border-line px-3 py-3.5 first:pl-1 last:pr-1">
              {row === 0 && column === 0 && (
                <span role="status" className="sr-only">
                  {label}
                </span>
              )}
              <Skeleton className={WIDTHS[(row + column) % WIDTHS.length]} />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}
