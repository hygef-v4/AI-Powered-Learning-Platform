"use client";

import { useCallback, useEffect, useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button, Field, Select, TextInput } from "@/shared/ui";
import { searchSubjectManagers, type ManagerCandidate, type SearchSubjectManagers } from "./managerSearch";
import { useSubjectRead } from "./useSubjectRead";

type Props = {
  value: ManagerCandidate | null;
  onChange: (manager: ManagerCandidate | null) => void;
  disabled?: boolean;
  searchManagers?: SearchSubjectManagers;
};

export function ManagerPicker({ value, onChange, disabled = false, searchManagers = searchSubjectManagers }: Props) {
  const id = useId();
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setQuery(search.trim()), 300);
    return () => clearTimeout(timer);
  }, [search]);
  const load = useCallback(() => searchManagers({ q: query, limit: 20 }), [query, searchManagers]);
  const resource = useSubjectRead(`manager-search:${query}`, load, !disabled);
  const waiting = search.trim() !== query;
  const failed = resource.error !== null;
  // Phòng response adapter sai; backend vẫn kiểm role/status và khóa account khi ghi.
  const candidates = (resource.data ?? []).filter((manager) => manager.role === "SUBJECT_MANAGER" && manager.status === "ACTIVE").slice(0, 20);
  const canSelect = !disabled && !waiting && !resource.loading && !failed;
  const status = resource.error instanceof ApiError ? resource.error.problem.status : null;

  return <fieldset disabled={disabled} className="flex min-w-0 flex-col gap-2" data-testid="subject-manager-picker">
    <legend className="mb-1 text-xs font-semibold text-body">Chủ nhiệm môn (tùy chọn)</legend>
    <p className="text-xs text-muted">{value ? `Đã chọn: ${value.displayName} (${value.email})` : "Chưa phân công"}</p>
    <Field label="Tìm chủ nhiệm theo tên hoặc email" htmlFor={`${id}-search`}>
      <TextInput id={`${id}-search`} value={search} maxLength={200} onChange={(event) => setSearch(event.target.value)} data-testid="subject-manager-search" />
    </Field>
    {(resource.loading || waiting) && !disabled && <p role="status" className="text-xs text-muted">Đang tìm chủ nhiệm…</p>}
    {!waiting && failed && <div role="alert" className="text-xs text-danger">
      <p>{status === 401 ? "Vui lòng đăng nhập lại để tìm chủ nhiệm." : status === 403 ? "Bạn không có quyền tìm chủ nhiệm." : "Không tải được danh sách chủ nhiệm. Có thể để chưa phân công và thử lại sau."}</p>
      <Button size="sm" onClick={resource.reload} data-testid="subject-manager-retry">Thử lại</Button>
    </div>}
    <Field label="Chọn chủ nhiệm" htmlFor={`${id}-select`} hint="Chỉ chọn tài khoản Chủ nhiệm môn đang hoạt động. Có thể phân công sau.">
      <Select id={`${id}-select`} value="" disabled={!canSelect} data-testid="subject-manager-select" onChange={(event) => {
        const manager = candidates.find((candidate) => candidate.accountId === event.target.value);
        if (canSelect && manager) onChange(manager);
      }}>
        <option value="">{canSelect && candidates.length === 0 ? "Không có chủ nhiệm phù hợp" : "Chọn từ kết quả tìm kiếm"}</option>
        {canSelect && candidates.map((manager) => <option key={manager.accountId} value={manager.accountId}>{manager.displayName} — {manager.email}</option>)}
      </Select>
    </Field>
    {value && <Button size="sm" onClick={() => onChange(null)} data-testid="subject-manager-clear">Bỏ lựa chọn chủ nhiệm</Button>}
  </fieldset>;
}
