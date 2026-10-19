"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button, Icon, TextInput } from "@/shared/ui";
import { searchSubjectManagers, type ManagerCandidate, type SearchSubjectManagers } from "./managerSearch";
import { useSubjectRead } from "./useSubjectRead";

type Props = {
  value: ManagerCandidate | null;
  onChange: (manager: ManagerCandidate | null) => void;
  disabled?: boolean;
  searchManagers?: SearchSubjectManagers;
};

const label = (manager: ManagerCandidate) => `${manager.displayName} · ${manager.email}`;

/** Một ô chọn Chủ nhiệm môn (design a06, a07c): gõ để tìm, chọn trong danh sách xổ xuống, nút x để bỏ chọn. */
export function ManagerPicker({ value, onChange, disabled = false, searchManagers = searchSubjectManagers }: Props) {
  const id = useId();
  const listRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
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
  // Phòng response adapter sai; backend vẫn kiểm role/status khi ghi.
  const candidates = (resource.data ?? []).filter((manager) => manager.role === "SUBJECT_MANAGER" && manager.status === "ACTIVE").slice(0, 20);
  const ready = !disabled && !waiting && !resource.loading && !failed;
  const status = resource.error instanceof ApiError ? resource.error.problem.status : null;

  function choose(manager: ManagerCandidate | null) {
    onChange(manager);
    setSearch("");
    setOpen(false);
  }

  return <div className="relative flex min-w-0 flex-col gap-1.5" data-testid="subject-manager-picker">
    <label htmlFor={`${id}-input`} className="text-xs font-semibold text-body">Chủ nhiệm môn <span className="font-normal text-muted">(tùy chọn)</span></label>
    <TextInput
      id={`${id}-input`}
      icon="search"
      role="combobox"
      aria-expanded={open}
      aria-controls={`${id}-list`}
      aria-autocomplete="list"
      autoComplete="off"
      maxLength={200}
      disabled={disabled}
      placeholder="Chưa phân công"
      value={open ? search : value ? label(value) : ""}
      onFocus={() => setOpen(true)}
      onChange={(event) => { setSearch(event.target.value); setOpen(true); }}
      onKeyDown={(event) => { if (event.key === "Escape") setOpen(false); }}
      onBlur={(event) => { if (!listRef.current?.contains(event.relatedTarget as Node | null)) setOpen(false); }}
      data-testid="subject-manager-search"
      suffix={value && !disabled ? <button type="button" aria-label="Bỏ chủ nhiệm" className="flex text-muted hover:text-ink" onMouseDown={(event) => event.preventDefault()} onClick={() => choose(null)} data-testid="subject-manager-clear"><Icon name="close" size={16} /></button> : undefined}
    />
    {open && !disabled && <div ref={listRef} id={`${id}-list`} role="listbox" aria-label="Chủ nhiệm môn"
      className="absolute top-[62px] z-20 max-h-64 w-full overflow-auto rounded-[9px] border border-line bg-white py-1 shadow-lg">
      {(resource.loading || waiting) && <p role="status" className="px-3 py-2 text-xs text-muted">Đang tìm chủ nhiệm…</p>}
      {ready && candidates.length === 0 && <p className="px-3 py-2 text-xs text-muted">Không có chủ nhiệm phù hợp</p>}
      {ready && candidates.map((manager) => <button key={manager.accountId} type="button" role="option" aria-selected={value?.accountId === manager.accountId}
        className="flex w-full flex-col px-3 py-2 text-left hover:bg-shell aria-selected:bg-primary-soft"
        onMouseDown={(event) => event.preventDefault()} onClick={() => choose(manager)}>
        <span className="text-[13px] font-semibold text-ink">{manager.displayName}</span>
        <span className="text-xs text-muted">{manager.email}</span>
      </button>)}
    </div>}
    {!waiting && failed && <div role="alert" className="flex items-center gap-2 text-xs text-danger">
      <p>{status === 401 ? "Vui lòng đăng nhập lại để tìm chủ nhiệm." : status === 403 ? "Bạn không có quyền tìm chủ nhiệm." : "Không tải được danh sách chủ nhiệm. Có thể để chưa phân công và thử lại sau."}</p>
      <Button size="sm" onClick={resource.reload} data-testid="subject-manager-retry">Thử lại</Button>
    </div>}
    <p className="text-[11px] text-muted">Chỉ hiện người có vai trò Chủ nhiệm môn đang hoạt động. Có thể phân công sau.</p>
  </div>;
}
