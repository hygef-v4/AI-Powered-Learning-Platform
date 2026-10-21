"use client";

import { Select, TextInput } from "@/shared/ui";
import { statusLabels, type AssignmentStatus, type GradingMode } from "./types";

export type FilterState = { q: string; status: AssignmentStatus | ""; gradingMode: GradingMode | ""; includeClosed: boolean };

export const emptyFilter: FilterState = { q: "", status: "", gradingMode: "", includeClosed: false };

/** Lọc theo chế độ, trạng thái, tìm theo tên; mặc định ẩn bài Đã đóng, Ngưng giao. */
export function AssignmentFilters({ value, onChange }: { value: FilterState; onChange: (value: FilterState) => void }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div role="tablist" aria-label="Chế độ" className="flex gap-1 rounded-[10px] bg-shell p-1">
        {([["", "Tất cả"], ["GRADED", "Graded"], ["PRACTICE", "Practice"]] as const).map(([mode, label]) => (
          <button key={label} type="button" role="tab" aria-selected={value.gradingMode === mode}
            onClick={() => onChange({ ...value, gradingMode: mode })}
            className={`rounded-lg px-3 py-1 text-xs font-semibold ${value.gradingMode === mode ? "bg-white text-ink shadow-sm" : "text-muted"}`}>
            {label}
          </button>
        ))}
      </div>
      <Select aria-label="Trạng thái" value={value.status} wrapperClassName="w-40"
        onChange={(event) => onChange({ ...value, status: event.target.value as AssignmentStatus | "" })}>
        <option value="">Mọi trạng thái</option>
        {(Object.keys(statusLabels) as AssignmentStatus[]).map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}
      </Select>
      <TextInput aria-label="Tìm bài tập" icon="search" placeholder="Tìm theo tên" value={value.q} wrapperClassName="w-56"
        onChange={(event) => onChange({ ...value, q: event.target.value })} />
      <label className="flex items-center gap-1.5 text-xs text-body">
        <input type="checkbox" checked={value.includeClosed} onChange={(event) => onChange({ ...value, includeClosed: event.target.checked })} />
        Hiện bài Đã đóng / Ngưng giao
      </label>
    </div>
  );
}

export function toListFilter(value: FilterState) {
  return {
    q: value.q.trim() || undefined,
    status: value.status ? [value.status] : undefined,
    gradingMode: value.gradingMode || undefined,
    includeClosed: value.includeClosed,
  };
}
