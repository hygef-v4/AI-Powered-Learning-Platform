import { cx } from "./cx";
import { Icon } from "./Icon";

/** Số trang hiện: trang đầu, cuối và hai bên trang đang mở; khoảng trống thành "…". */
export function pageWindow(current: number, total: number): (number | "gap")[] {
  const pages = new Set([0, total - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < total));
  const sorted = [...pages].sort((a, b) => a - b);
  const result: (number | "gap")[] = [];
  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1]! > 1) result.push("gap");
    result.push(page);
  });
  return result;
}

/** Nút trang ‹ 1 2 … n › ở cuối bảng danh sách quản trị, căn giữa; một trang thì không hiện. Trang đếm từ 0. */
export function PageNav({
  page,
  size,
  total,
  onPage,
}: {
  page: number;
  size: number;
  total: number;
  /** Giữ để tương thích, không còn dòng "Hiển thị a–b trong N". */
  unit?: string;
  onPage: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  if (totalPages <= 1) return null;
  return (
    <div className="flex justify-center px-1 text-xs text-muted">
      <nav aria-label="Phân trang" className="flex items-center gap-1">
        <PageButton label="Trang trước" disabled={page === 0} onClick={() => onPage(page - 1)}>
          <Icon name="chevron_right" size={16} className="rotate-180" />
        </PageButton>
        {pageWindow(page, totalPages).map((item, index) =>
          item === "gap" ? (
            <span key={`gap-${index}`} className="px-1 text-faint">
              …
            </span>
          ) : (
            <PageButton key={item} label={`Trang ${item + 1}`} current={item === page} onClick={() => onPage(item)}>
              {item + 1}
            </PageButton>
          ),
        )}
        <PageButton label="Trang sau" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          <Icon name="chevron_right" size={16} />
        </PageButton>
      </nav>
    </div>
  );
}

function PageButton({
  label,
  current,
  disabled,
  onClick,
  children,
}: {
  label: string;
  current?: boolean;
  disabled?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-current={current ? "page" : undefined}
      disabled={disabled}
      onClick={onClick}
      className={cx(
        "inline-flex h-7 min-w-7 items-center justify-center rounded-md border px-1.5 text-xs font-semibold disabled:opacity-40",
        current ? "border-primary bg-primary text-on-primary" : "border-line-strong bg-surface text-body hover:bg-shell",
      )}
    >
      {children}
    </button>
  );
}
