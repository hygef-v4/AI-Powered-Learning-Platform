"use client";

import { TextInput, cx } from "@/shared/ui";
import type { SubjectStatus } from "./types";

type Props = {
  search: string; status: SubjectStatus; counts: Record<SubjectStatus, number> | null;
  onSearch: (value: string) => void; onStatus: (value: SubjectStatus) => void;
};

export function SubjectFilters({ search, status, counts, onSearch, onStatus }: Props) {
  return <div className="flex flex-wrap items-center justify-between gap-3">
    <div role="group" aria-label="Trạng thái môn học" className="flex gap-1 rounded-full bg-neutral-soft p-1">
      {(["ACTIVE", "ARCHIVED"] as const).map((value) => <button key={value} type="button"
        aria-pressed={status === value} data-testid={`subjects-filter-${value}`} onClick={() => onStatus(value)}
        className={cx("rounded-full px-3 py-1.5 text-xs font-semibold focus-visible:outline-2 focus-visible:outline-primary", status === value ? "bg-ink text-white" : "text-muted hover:text-ink")}>
        {value === "ACTIVE" ? "Đang hoạt động" : "Lưu trữ"} ({counts?.[value] ?? "—"})
      </button>)}
    </div>
    <TextInput aria-label="Tìm môn theo mã hoặc tên" placeholder="Tìm theo mã hoặc tên môn"
      value={search} maxLength={200} data-testid="subjects-search"
      onChange={(event) => onSearch(event.target.value)} wrapperClassName="w-full sm:w-72" />
  </div>;
}
