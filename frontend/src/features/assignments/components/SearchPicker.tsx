"use client";

import { useLayoutEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Icon, type IconName } from "@/shared/ui";

export type PickerOption = { id: string; label: string };

type Props = {
  id: string;
  /** Tên của ô (aria-label). */
  label: string;
  placeholder?: string;
  options: PickerOption[];
  onPick: (id: string) => void;
  /** Lần đầu bấm vào ô: tải danh sách. */
  onOpen?: () => void;
  disabled?: boolean;
  loading?: boolean;
  emptyText?: string;
  optionIcon?: IconName;
  /** Dòng cuối của gợi ý (ví dụ tải tài liệu lên). */
  footer?: { label: string; icon: IconName; disabled?: boolean; onPick: () => void };
  /** Chọn nhiều: chọn xong gợi ý vẫn mở để thêm tiếp. */
  multiple?: boolean;
  /** Các mục đã chọn, xếp hàng ngay trong thanh tìm (trước chỗ gõ), dài thì xuống dòng. */
  chips?: ReactNode;
};

const MAX_HEIGHT = 256;

/**
 * Ô tìm rồi chọn trong danh sách (yêu cầu 2026-11-05): gõ để lọc, gợi ý luôn nằm ngay dưới ô tìm, nổi trên popup (fixed,
 * ngoài vùng cuộn) nên mở gợi ý không làm popup dài ra; gợi ý dài thì cuộn trong khung gợi ý, không tràn lên trên ô tìm.
 * Dùng cho Tài liệu tham khảo (Soạn bằng AI) và Copy từ bài có sẵn (Tạo bài tập).
 */
export function SearchPicker({ id, label, placeholder, options, onPick, onOpen, disabled, loading, emptyText = "Không có kết quả.",
  optionIcon, footer, multiple = false, chips }: Props) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLDivElement>(null);
  const [place, setPlace] = useState<{ top: number; left: number; width: number; maxHeight: number } | null>(null);
  // Popup căn giữa màn hình: chọn tài liệu làm popup cao thêm thì ô tìm dịch lên. Bám theo ô tìm mỗi khung hình khi gợi ý
  // đang mở để gợi ý luôn sát ngay dưới ô tìm (lỗi 2026-11-05).
  useLayoutEffect(() => {
    if (!open) return;
    let frame = 0;
    let last = "";
    const follow = () => {
      const box = anchor.current?.getBoundingClientRect();
      if (box) {
        const top = box.bottom + 4;
        const next = { top, left: box.left, width: box.width,
          maxHeight: Math.max(120, Math.min(MAX_HEIGHT, window.innerHeight - top - 12)) };
        const key = `${next.top}|${next.left}|${next.width}|${next.maxHeight}`;
        if (key !== last) {
          last = key;
          setPlace(next);
        }
      }
      frame = requestAnimationFrame(follow);
    };
    frame = requestAnimationFrame(follow);
    return () => cancelAnimationFrame(frame);
  }, [open]);
  const needle = query.trim().toLocaleLowerCase("vi");
  const shown = needle ? options.filter((option) => option.label.toLocaleLowerCase("vi").includes(needle)) : options;
  const listId = `${id}-list`;
  function show() {
    onOpen?.();
    setOpen(true);
  }
  return (
    <div ref={anchor}>
      <div className={"flex min-h-10 flex-wrap items-center gap-1.5 rounded-lg border border-field bg-surface px-3 py-1.5 "
        + "transition-[border-color,box-shadow] duration-200 focus-within:border-focus "
        + "focus-within:shadow-[0_0_0_3px_var(--c-primary-soft)] has-[:disabled]:bg-sunken has-[:disabled]:opacity-70"}>
        <Icon name="search" size={16} className="text-muted" />
        {chips}
        <input id={id} aria-label={label} placeholder={placeholder} role="combobox" aria-expanded={open}
          aria-controls={listId} autoComplete="off" value={query} disabled={disabled}
          className="h-7 min-w-[8rem] flex-1 bg-transparent text-[13.5px] text-ink outline-none placeholder:text-muted disabled:cursor-not-allowed"
          onChange={(e) => { setQuery(e.target.value); show(); }}
          onFocus={show} onMouseDown={show} onBlur={() => setOpen(false)}
          onKeyDown={(e) => { if (e.key === "Escape") setOpen(false); }} />
      </div>
      {open && !disabled && typeof document !== "undefined" && createPortal(
        <ul id={listId} role="listbox" aria-label={`Gợi ý ${label.toLocaleLowerCase("vi")}`}
          style={place ? { top: place.top, left: place.left, width: place.width, maxHeight: place.maxHeight } : { visibility: "hidden" }}
          className="fixed z-[110] flex flex-col overflow-y-auto rounded-lg border border-line bg-surface py-1 shadow-[var(--shadow-float)]">
          {shown.map((option) => (
            <li key={option.id} role="option" aria-selected={false}
              onMouseDown={(e) => { e.preventDefault(); onPick(option.id); setQuery(""); if (!multiple) setOpen(false); }}
              className="flex shrink-0 cursor-pointer items-center gap-2 px-3 py-2 text-[13px] text-ink hover:bg-canvas">
              {optionIcon && <Icon name={optionIcon} size={15} className="text-muted" />}
              <span className="min-w-0 truncate">{option.label}</span>
            </li>
          ))}
          {shown.length === 0 && (
            <li className="shrink-0 px-3 py-2 text-xs text-muted">{loading ? "Đang tải…" : emptyText}</li>
          )}
          {footer && (
            <li role="option" aria-selected={false} aria-disabled={footer.disabled}
              onMouseDown={(e) => { e.preventDefault(); if (footer.disabled) return; setOpen(false); footer.onPick(); }}
              className={`flex shrink-0 items-center gap-2 border-t border-line px-3 py-2 text-[13px] font-semibold ${
                footer.disabled ? "cursor-not-allowed text-faint" : "cursor-pointer text-primary-ink hover:bg-canvas"}`}>
              <Icon name={footer.icon} size={15} />
              {footer.label}
            </li>
          )}
        </ul>,
        document.body,
      )}
    </div>
  );
}
