"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { ApiError } from "@/shared/api/client";
import { Button, Icon, TextInput } from "@/shared/ui";
import { searchSubjectManagers, type ManagerCandidate, type SearchSubjectManagers } from "../model/managerSearch";
import { useSubjectRead } from "../hooks/useSubjectRead";

type Props = {
  value: ManagerCandidate | null;
  onChange: (manager: ManagerCandidate | null) => void;
  disabled?: boolean;
  searchManagers?: SearchSubjectManagers;
};

/** Đã chọn thì ô chỉ ghi email; danh sách gợi ý vẫn có tên và email. */
const label = (manager: ManagerCandidate) => manager.email;

/**
 * Một ô chọn Chủ nhiệm môn (design a06, a07c): gõ để tìm, chọn trong danh sách xổ xuống, nút x để bỏ chọn. Danh sách nổi
 * đè lên phần bên dưới (vẽ ở body, bám theo ô nhập) nên không đẩy form xuống và không bị popup cắt mất.
 */
export function ManagerPicker({ value, onChange, disabled = false, searchManagers = searchSubjectManagers }: Props) {
  const id = useId();
  const listRef = useRef<HTMLDivElement>(null);
  const anchorRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [place, setPlace] = useState<{ top: number; left: number; width: number } | null>(null);
  // Bám vị trí ô nhập khi mở, khi cuộn hoặc đổi cỡ cửa sổ.
  useEffect(() => {
    if (!open) return;
    const update = () => {
      const rect = anchorRef.current?.getBoundingClientRect();
      if (rect) setPlace({ top: rect.bottom + 4, left: rect.left, width: rect.width });
    };
    const frame = requestAnimationFrame(update);
    window.addEventListener("resize", update);
    window.addEventListener("scroll", update, true);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", update);
      window.removeEventListener("scroll", update, true);
    };
  }, [open]);
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

  return <div className="flex min-w-0 flex-col gap-1.5" data-testid="subject-manager-picker">
    <label htmlFor={`${id}-input`} className="text-xs font-semibold text-body">Chủ nhiệm môn</label>
    <div ref={anchorRef}>
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
    </div>
    {open && !disabled && typeof document !== "undefined" && createPortal(<div ref={listRef} id={`${id}-list`} role="listbox" aria-label="Chủ nhiệm môn"
      style={place ? { top: place.top, left: place.left, width: place.width } : { visibility: "hidden" }}
      className="fixed z-[60] max-h-56 overflow-auto rounded-lg border border-line bg-surface py-1 shadow-[var(--shadow-float)]">
      {(resource.loading || waiting) && <p role="status" className="px-3 py-2 text-xs text-muted">Đang tìm chủ nhiệm…</p>}
      {ready && candidates.length === 0 && <p className="px-3 py-2 text-xs text-muted">Không có chủ nhiệm phù hợp</p>}
      {ready && candidates.map((manager) => <button key={manager.accountId} type="button" role="option" aria-selected={value?.accountId === manager.accountId}
        className="flex w-full flex-col px-3 py-2 text-left hover:bg-shell aria-selected:bg-primary-soft"
        onMouseDown={(event) => event.preventDefault()} onClick={() => choose(manager)}>
        <span className="text-[13px] font-semibold text-ink">{manager.displayName}</span>
        <span className="text-xs text-muted">{manager.email}</span>
      </button>)}
    </div>, document.body)}
    {!waiting && failed && <div role="alert" className="flex items-center gap-2 text-xs text-danger">
      <p>{status === 401 ? "Vui lòng đăng nhập lại để tìm chủ nhiệm." : status === 403 ? "Bạn không có quyền tìm chủ nhiệm." : "Không tải được danh sách chủ nhiệm. Có thể để chưa phân công và thử lại sau."}</p>
      <Button size="sm" onClick={resource.reload} data-testid="subject-manager-retry">Thử lại</Button>
    </div>}
  </div>;
}
