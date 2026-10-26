import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";

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

/** Dòng chân bảng (design `a02`): "Hiển thị a–b trong N <đơn vị>" và nút trang ‹ 1 2 … n ›. Trang đếm từ 0. */
export function Pagination({
  page,
  size,
  total,
  unit,
  onPage,
}: {
  page: number;
  size: number;
  total: number;
  unit: string;
  onPage: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 px-1 text-xs text-muted">
      <span>
        Hiển thị {from}–{to} trong {total} {unit}
      </span>
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
