"use client";

import { Segmented, TextInput } from "@/shared/ui";
import type { SubjectStatus } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = {
  search: string;
  /** Không có là Tất cả. */
  status: SubjectStatus | undefined;
  onSearch: (value: string) => void;
  onStatus: (value: SubjectStatus | undefined) => void;
};

const TABS: { value: SubjectStatus | undefined; key: string; label: string }[] = [
  { value: undefined, key: "ALL", label: msg("subjects.components.SubjectFilters.all") },
  { value: "ACTIVE", key: "ACTIVE", label: msg("subjects.components.SubjectFilters.active") },
  { value: "ARCHIVED", key: "ARCHIVED", label: msg("subjects.components.SubjectFilters.archived") },
];

/** Tab trạng thái bên trái, ô tìm ngắn bên phải trên cùng một hàng. */
export function SubjectFilters({ search, status, onSearch, onStatus }: Props) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Segmented
        as="tab"
        ariaLabel={msg("subjects.components.SubjectFilters.subjectStatus")}
        value={status ?? "ALL"}
        onChange={(key) => onStatus(TABS.find((tab) => tab.key === key)?.value)}
        options={TABS.map((tab) => ({ value: tab.key, label: tab.label, testId: `subjects-filter-${tab.key}` }))}
      />
      <div role="search" className="ml-auto w-full sm:w-72">
        <TextInput
          type="search"
          icon="search"
          aria-label={msg("subjects.components.SubjectFilters.filterBySubjectCodeOr")}
          placeholder={msg("subjects.components.SubjectFilters.filterBySubjectCodeOr")}
          value={search}
          maxLength={200}
          data-testid="subjects-search"
          onChange={(event) => onSearch(event.target.value)}
        />
      </div>
    </div>
  );
}
