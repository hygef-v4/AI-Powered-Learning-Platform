"use client";
import { cx } from "./cx";
import { Icon } from "./Icon";
import { Menu } from "./Menu";
import { msg } from "@/i18n/msg";

/**
 * Ô lọc theo học kỳ của lớp (D-U04-05), cùng kiểu `Menu` với `ClassFilter`: "Tất cả học kỳ" rồi các học kỳ đã có, mới trước.
 * Giá trị rỗng là Tất cả; không có lựa chọn "Chưa có học kỳ" vì mọi lớp đều có học kỳ (BR-U05-67).
 */
export function TermFilter({ options, value, onChange, testId }: { options: readonly string[]; value: string; onChange: (term: string) => void; testId?: string }) {
  const buildOption = (term: string, label: string) => {
    const selected = term === value;
    return (
      <button key={term || "all"} type="button" role="menuitem" data-menu-item aria-current={selected ? "true" : undefined} onClick={() => onChange(term)}
        className={cx("flex w-full items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-[13px] text-ink transition-colors hover:bg-sunken", selected && "bg-primary-soft")}>
        <span className={cx("min-w-0 flex-1 truncate", term && "font-mono")}>{label}</span>
        {selected && <Icon name="check" size={15} className="shrink-0 text-primary-ink" />}
      </button>
    );
  };
  return (
    <div data-testid={testId}>
      <Menu ariaLabel={msg("shared.ui.TermFilter.filterByTerm")} variant="tool" align="right" className="w-full"
        menuClassName="left-0 min-w-0"
        summaryClassName="!h-10 w-full rounded-lg border border-line bg-surface !px-2.5 text-left text-[13px] hover:!bg-surface hover:border-line-strong [&>svg:last-child]:ml-auto [&>svg:last-child]:shrink-0"
        label={<span className={cx("min-w-0 flex-1 truncate text-ink", value && "font-mono")}>{value || msg("shared.ui.TermFilter.allTerms")}</span>}>
        <div className="flex max-h-72 w-full flex-col gap-0.5 overflow-y-auto">
          {buildOption("", msg("shared.ui.TermFilter.allTerms"))}
          {options.map((term) => buildOption(term, term))}
        </div>
      </Menu>
    </div>
  );
}
