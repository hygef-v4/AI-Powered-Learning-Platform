import type { ReactNode } from "react";
import { cx } from "./cx";
import { Icon } from "./Icon";

type Props = {
  /** Trang hiện tại, bắt đầu từ 0. */
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  /** Đơn vị đếm trong dòng "Hiển thị a–b trong N …", ví dụ "bản ghi". */
  itemLabel?: string;
  /** Chữ thêm sau dòng hiển thị, ví dụ "50 dòng mỗi trang". */
  note?: ReactNode;
  /** false: ẩn dòng "Hiển thị a–b trong N", chỉ còn nút chuyển trang. */
  summary?: boolean;
  testId?: string;
};

const NUMBER = new Intl.NumberFormat("vi-VN");

/** Các trang cần hiện: đầu, cuối, quanh trang hiện tại; khoảng trống thành "…". */
export function pageItems(current: number, count: number): (number | "gap")[] {
  if (count <= 7) return Array.from({ length: count }, (_, i) => i);
  const wanted = new Set([0, count - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < count));
  if (current <= 2) [1, 2, 3].forEach((p) => wanted.add(p));
  if (current >= count - 3) [count - 4, count - 3, count - 2].forEach((p) => wanted.add(p));
  const sorted = [...wanted].sort((a, b) => a - b);
  const items: (number | "gap")[] = [];
  let previous: number | undefined;
  for (const p of sorted) {
    if (previous !== undefined && p - previous > 1) items.push("gap");
    items.push(p);
    previous = p;
  }
  return items;
}

const BOX = "inline-flex h-[30px] min-w-[30px] items-center justify-center rounded-lg px-2 text-xs font-semibold transition-colors duration-200";

/** Dòng "Hiển thị a–b trong N …" và nút chuyển trang, theo design. */
export function Pagination({ page, pageSize, total, onPageChange, itemLabel = "bản ghi", note, summary: showSummary = true, testId = "pagination" }: Props) {
  const count = Math.max(1, Math.ceil(total / pageSize));
  const from = total === 0 ? 0 : page * pageSize + 1;
  const to = Math.min(total, (page + 1) * pageSize);
  const summary =
    total === 0 ? `Không có ${itemLabel}` : `Hiển thị ${NUMBER.format(from)}–${NUMBER.format(to)} trong ${NUMBER.format(total)} ${itemLabel}`;

  return (
    <div className={cx("flex flex-wrap items-center gap-3", showSummary ? "justify-between" : "justify-center")} data-testid={testId}>
      {showSummary && (
        <p className="text-[12.5px] text-muted">
          {summary}
          {note && <> · {note}</>}
        </p>
      )}
      {count > 1 && (
        <nav aria-label="Phân trang" className="flex items-center gap-1.5">
          <button
            type="button"
            aria-label="Trang trước"
            disabled={page === 0}
            onClick={() => onPageChange(page - 1)}
            className={cx(BOX, "text-body hover:bg-sunken disabled:cursor-not-allowed disabled:opacity-35")}
            data-testid={`${testId}-prev-button`}
          >
            <Icon name="chevron_left" size={16} />
          </button>
          {pageItems(page, count).map((item, i) =>
            item === "gap" ? (
              <span key={`gap-${i}`} className={cx(BOX, "text-muted")} aria-hidden>
                …
              </span>
            ) : (
              <button
                key={item}
                type="button"
                aria-current={item === page ? "page" : undefined}
                onClick={() => onPageChange(item)}
                className={cx(BOX, item === page ? "bg-primary-soft font-mono text-ink" : "font-mono text-muted hover:bg-sunken hover:text-ink")}
                data-testid={`${testId}-page-${item + 1}-button`}
              >
                {item + 1}
              </button>
            ),
          )}
          <button
            type="button"
            aria-label="Trang sau"
            disabled={page >= count - 1}
            onClick={() => onPageChange(page + 1)}
            className={cx(BOX, "text-body hover:bg-sunken disabled:cursor-not-allowed disabled:opacity-35")}
            data-testid={`${testId}-next-button`}
          >
            <Icon name="chevron_right" size={16} />
          </button>
        </nav>
      )}
    </div>
  );
}
