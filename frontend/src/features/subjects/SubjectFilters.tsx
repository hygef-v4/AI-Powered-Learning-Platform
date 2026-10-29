"use client";

import { TextInput, cx } from "@/shared/ui";
import type { SubjectStatus } from "./types";

type Props = {
  search: string;
  /** Không có là Tất cả. */
  status: SubjectStatus | undefined;
  onSearch: (value: string) => void;
  onStatus: (value: SubjectStatus | undefined) => void;
};

const TABS: { value: SubjectStatus | undefined; key: string; label: string }[] = [
  { value: undefined, key: "ALL", label: "Tất cả" },
  { value: "ACTIVE", key: "ACTIVE", label: "Hoạt động" },
  { value: "ARCHIVED", key: "ARCHIVED", label: "Lưu trữ" },
];

/** Tab trạng thái bên trái, ô tìm ngắn bên phải trên cùng một hàng. */
export function SubjectFilters({ search, status, onSearch, onStatus }: Props) {
  return <div className="flex flex-wrap items-center gap-3">
    <div role="tablist" aria-label="Trạng thái môn học" className="flex flex-wrap gap-1 rounded-xl bg-shell p-1">
      {TABS.map((tab) => <button key={tab.key} type="button" role="tab" aria-selected={status === tab.value}
        data-testid={`subjects-filter-${tab.key}`} onClick={() => onStatus(tab.value)}
        className={cx("rounded-[7px] px-2.5 py-1.5 text-xs font-semibold transition-colors duration-200",
          status === tab.value ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
        {tab.label}
      </button>)}
    </div>
    <div role="search" className="ml-auto w-full sm:w-72">
      <TextInput type="search" icon="search" aria-label="Lọc theo mã hoặc tên môn" placeholder="Lọc theo mã hoặc tên môn"
        value={search} maxLength={200} data-testid="subjects-search" onChange={(event) => onSearch(event.target.value)} />
    </div>
  </div>;
}
