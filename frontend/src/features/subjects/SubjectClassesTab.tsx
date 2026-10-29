"use client";

import { useState } from "react";
import { TextInput, cx } from "@/shared/ui";
import { SubjectClassesTable } from "./SubjectClassesTable";
import type { SubjectClassStatus, SubjectClassSummary } from "./types";

const TABS: { value: SubjectClassStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "DRAFT", label: "Nháp" },
  { value: "OPEN", label: "Đang mở" },
  { value: "ARCHIVED", label: "Lưu trữ" },
];

/** Lớp của môn (chỉ xem), cùng kiểu danh sách Môn học: tab trạng thái bên trái, ô tìm theo mã hoặc tên lớp bên phải. */
export function SubjectClassesTab({ items }: { items: SubjectClassSummary[] }) {
  const [status, setStatus] = useState<SubjectClassStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const q = search.trim().toLocaleLowerCase("vi");
  const filtered = items.filter((item) => (status === "ALL" || item.status === status)
    && (!q || item.code.toLocaleLowerCase("vi").includes(q) || item.name.toLocaleLowerCase("vi").includes(q)));
  return <section className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
    <div className="flex flex-wrap items-center gap-3">
      <div role="tablist" aria-label="Trạng thái lớp" className="flex flex-wrap gap-1 rounded-xl bg-shell p-1">
        {TABS.map((tab) => <button key={tab.value} type="button" role="tab" aria-selected={status === tab.value}
          data-testid={`subjects-classes-filter-${tab.value}`} onClick={() => setStatus(tab.value)}
          className={cx("rounded-[7px] px-2.5 py-1.5 text-xs font-semibold transition-colors duration-200",
            status === tab.value ? "bg-surface text-ink shadow-sm" : "text-muted hover:text-ink")}>
          {tab.label}
        </button>)}
      </div>
      <div role="search" className="ml-auto w-full sm:w-72">
        <TextInput type="search" icon="search" aria-label="Lọc theo mã hoặc tên lớp" placeholder="Lọc theo mã hoặc tên lớp"
          value={search} maxLength={200} data-testid="subjects-classes-search" onChange={(event) => setSearch(event.target.value)} />
      </div>
    </div>
    <SubjectClassesTable items={filtered} />
  </section>;
}
