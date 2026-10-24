"use client";

import { useState } from "react";
import { Button, Select } from "@/shared/ui";
import { SubjectClassesTable } from "./SubjectClassesTable";
import type { SubjectClassStatus, SubjectClassSummary } from "./types";

export function SubjectClassesTab({ items }: { items: SubjectClassSummary[] }) {
  const [status, setStatus] = useState<SubjectClassStatus | "ALL">("ALL");
  const [term, setTerm] = useState("");
  const filtered = items.filter((item) => (status === "ALL" || item.status === status) && (!term || item.term === term));
  return <div className="flex flex-col gap-3">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div role="group" aria-label="Trạng thái lớp" className="flex flex-wrap gap-1">
        {(["ALL", "DRAFT", "OPEN", "ARCHIVED"] as const).map((value) => <Button key={value} size="sm" variant={status === value ? "primary" : "ghost"}
          aria-pressed={status === value} data-testid={`subjects-classes-filter-${value}`} onClick={() => setStatus(value)}>
          {value === "ALL" ? "Tất cả" : value === "DRAFT" ? "Nháp" : value === "OPEN" ? "Đang mở" : "Lưu trữ"} ({value === "ALL" ? items.length : items.filter((item) => item.status === value).length})
        </Button>)}
      </div>
      <Select aria-label="Lọc lớp theo học kỳ" value={term} onChange={(event) => setTerm(event.target.value)} data-testid="subjects-classes-term">
        <option value="">Tất cả học kỳ</option>
        {[...new Set(items.map((item) => item.term))].sort().map((value) => <option key={value} value={value}>{value}</option>)}
      </Select>
    </div>
    <p className="text-sm text-muted">Chỉ xem. Lớp do Chủ nhiệm môn quản lý.</p>
    <SubjectClassesTable items={filtered} />
    <p className="text-xs text-muted">{filtered.length} / {items.length} lớp</p>
  </div>;
}
