"use client";

import { Segmented, TextInput } from "@/shared/ui";
import type { SubjectStatus } from "../model/types";

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
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Segmented
        as="tab"
        ariaLabel="Trạng thái môn học"
        value={status ?? "ALL"}
        onChange={(key) => onStatus(TABS.find((tab) => tab.key === key)?.value)}
        options={TABS.map((tab) => ({ value: tab.key, label: tab.label, testId: `subjects-filter-${tab.key}` }))}
      />
      <div role="search" className="ml-auto w-full sm:w-72">
        <TextInput
          type="search"
          icon="search"
          aria-label="Lọc theo mã hoặc tên môn"
          placeholder="Lọc theo mã hoặc tên môn"
          value={search}
          maxLength={200}
          data-testid="subjects-search"
          onChange={(event) => onSearch(event.target.value)}
        />
      </div>
    </div>
  );
}
