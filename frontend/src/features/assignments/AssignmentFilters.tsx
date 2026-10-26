"use client";

import { Select, TextInput } from "@/shared/ui";
import { statusLabels, type AssignmentStatus, type GradingMode } from "./types";

export type FilterState = { q: string; status: AssignmentStatus | ""; gradingMode: GradingMode | ""; includeClosed: boolean };

export const emptyFilter: FilterState = { q: "", status: "", gradingMode: "", includeClosed: false };

/** Lọc theo chế độ, trạng thái, tìm theo tên; mặc định "Đang dùng" ẩn bài Đã đóng, Ngưng giao. */
export function AssignmentFilters({ value, onChange }: { value: FilterState; onChange: (value: FilterState) => void }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div role="tablist" aria-label="Chế độ" className="flex gap-1 rounded-xl bg-shell p-1">
        {([["", "Tất cả"], ["GRADED", "Tính điểm"], ["PRACTICE", "Luyện tập"]] as const).map(([mode, label]) => (
          <button key={label} type="button" role="tab" aria-selected={value.gradingMode === mode}
            onClick={() => onChange({ ...value, gradingMode: mode })}
            className={`rounded-lg px-3 py-1 text-xs font-semibold ${value.gradingMode === mode ? "bg-surface text-ink shadow-sm" : "text-muted"}`}>
            {label}
          </button>
        ))}
      </div>
      <Select aria-label="Trạng thái" value={value.status || (value.includeClosed ? "*" : "")} wrapperClassName="w-44"
        onChange={(event) => {
          const picked = event.target.value;
          // Chọn một trạng thái cụ thể thì luôn lấy cả bài đã đóng; mặc định chỉ bài đang dùng.
          onChange({ ...value, status: picked === "*" ? "" : picked as AssignmentStatus | "", includeClosed: picked !== "" });
        }}>
        <option value="">Đang dùng</option>
        <option value="*">Tất cả</option>
        {(Object.keys(statusLabels) as AssignmentStatus[]).filter((status) => status !== "REVIEWED")
          .map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}
      </Select>
      <TextInput aria-label="Tìm bài tập" icon="search" placeholder="Tìm theo tên" value={value.q} wrapperClassName="w-56"
        onChange={(event) => onChange({ ...value, q: event.target.value })} />
    </div>
  );
}

/** Có điều kiện lọc nào khác mặc định không (để báo "không khớp bộ lọc" thay vì "chưa có bài"). */
export function isFiltered(value: FilterState) {
  return Boolean(value.q.trim() || value.status || value.gradingMode);
}

export function toListFilter(value: FilterState) {
  return {
    q: value.q.trim() || undefined,
    // "Nháp" gồm cả bài đã duyệt từ trước (không còn trạng thái Đã duyệt).
    status: value.status === "DRAFT" ? ["DRAFT", "REVIEWED"] as AssignmentStatus[] : value.status ? [value.status] : undefined,
    gradingMode: value.gradingMode || undefined,
    includeClosed: value.includeClosed,
  };
}
