"use client";

import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon, TextInput } from "@/shared/ui";
import { useCandidates, type CandidateLoader } from "./useCandidates";
import type { PersonView } from "./types";

type Props = {
  label: string;
  value: PersonView | null;
  onChange: (person: PersonView | null) => void;
  /** Danh sách có sẵn khi không có loader (test, dữ liệu trang). */
  people?: PersonView[];
  loader?: CandidateLoader;
  /** Cho bỏ chọn bằng nút x. */
  clearable?: boolean;
  disabled?: boolean;
  placeholder?: string;
  required?: boolean;
  invalid?: boolean;
};

/**
 * Ô chọn một người (giảng viên, sinh viên), cùng kiểu ô chọn Chủ nhiệm môn: gõ tên hoặc email, gợi ý nổi đè lên phần bên
 * dưới (vẽ ở body, bám theo ô nhập); đã chọn thì ô chỉ ghi email.
 */
export function PersonPicker({ label, value, onChange, people = [], loader, clearable = false, disabled = false, placeholder, required = false, invalid = false }: Props) {
  const id = useId();
  const anchorRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [place, setPlace] = useState<{ top: number; left: number; width: number } | null>(null);
  const options = useCandidates(people, search, loader);
  const q = search.trim().toLocaleLowerCase("vi");
  const rows = options.rows.filter((person) => !q || `${person.name} ${person.email}`.toLocaleLowerCase("vi").includes(q)).slice(0, 20);

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

  function choose(person: PersonView | null) {
    onChange(person);
    setSearch("");
    setOpen(false);
  }

  return <div className="flex min-w-0 flex-col gap-1.5">
    <label htmlFor={`${id}-input`} className="text-xs font-semibold text-body">{label}{required && <span className="text-primary-ink"> *</span>}</label>
    <div ref={anchorRef}>
      <TextInput id={`${id}-input`} icon="search" role="combobox" invalid={invalid} aria-expanded={open} aria-controls={`${id}-list`} aria-autocomplete="list"
        autoComplete="off" maxLength={200} disabled={disabled} placeholder={placeholder ?? "Gõ tên hoặc email"}
        value={open ? search : value?.email ?? ""}
        onFocus={() => setOpen(true)}
        onChange={(event) => { setSearch(event.target.value); setOpen(true); }}
        onKeyDown={(event) => { if (event.key === "Escape" && open) { event.stopPropagation(); setOpen(false); } }}
        onBlur={(event) => { if (!listRef.current?.contains(event.relatedTarget as Node | null)) setOpen(false); }}
        suffix={clearable && value && !disabled ? <button type="button" aria-label={`Bỏ chọn ${label.toLocaleLowerCase("vi")}`} className="flex text-muted hover:text-ink"
          onMouseDown={(event) => event.preventDefault()} onClick={() => choose(null)}><Icon name="close" size={16} /></button> : undefined} />
    </div>
    {open && !disabled && typeof document !== "undefined" && createPortal(<div ref={listRef} id={`${id}-list`} role="listbox" aria-label={label}
      style={place ? { top: place.top, left: place.left, width: place.width } : { visibility: "hidden" }}
      className="fixed z-[60] max-h-56 overflow-auto rounded-lg border border-line bg-surface py-1 shadow-[var(--shadow-float)]">
      {options.loading && <p role="status" className="px-3 py-2 text-xs text-muted">Đang tìm…</p>}
      {options.error && <p role="alert" className="px-3 py-2 text-xs text-danger">{options.error}</p>}
      {!options.loading && !options.error && rows.length === 0 && <p className="px-3 py-2 text-xs text-muted">Không có người phù hợp</p>}
      {!options.loading && rows.map((person) => <button key={person.id} type="button" role="option" aria-selected={value?.id === person.id}
        className="flex w-full flex-col px-3 py-2 text-left hover:bg-shell aria-selected:bg-primary-soft"
        onMouseDown={(event) => event.preventDefault()} onClick={() => choose(person)}>
        <span className="text-[13px] font-semibold text-ink">{person.name}</span>
        <span className="text-xs text-muted">{person.email}</span>
      </button>)}
    </div>, document.body)}
  </div>;
}
