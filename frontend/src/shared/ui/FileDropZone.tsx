"use client";

import { useRef, useState, type DragEvent, type ReactNode } from "react";
import { msg } from "@/i18n/msg";
import { cx } from "./cx";
import { Icon } from "./Icon";
import { showErrorToast } from "./Toast";

/** Tệp có khớp {@code accept} của ô chọn tệp không (đuôi ".docx" hay kiểu "image/*"); bỏ trống là nhận mọi tệp. */
export function matchesAccept(file: File, accept?: string): boolean {
  if (!accept) return true;
  const name = file.name.toLowerCase();
  return accept.split(",").map((rule) => rule.trim().toLowerCase()).filter(Boolean).some((rule) =>
    rule.startsWith(".") ? name.endsWith(rule)
      : rule.endsWith("/*") ? file.type.toLowerCase().startsWith(rule.slice(0, -1))
        : file.type.toLowerCase() === rule);
}

const hasFiles = (event: DragEvent) => Array.from(event.dataTransfer?.types ?? []).includes("Files");

/**
 * Vùng nhận tệp kéo thả (yêu cầu 2026-11-27): bọc một ô chọn tệp có sẵn (ô tìm mẫu đề, ô tài liệu tham khảo); khi kéo tệp
 * vào thì phủ một lớp viền đứt và nhãn {@code label}. Chỉ nhận tệp khớp {@code accept}, tệp sai loại thì báo lỗi; {@code multiple} sai thì lấy tệp đầu.
 */
export function FileDropZone({ accept, multiple = false, disabled = false, label, onFiles, children, className }: {
  accept?: string;
  multiple?: boolean;
  disabled?: boolean;
  label: string;
  onFiles: (files: File[]) => void;
  children: ReactNode;
  className?: string;
}) {
  const [over, setOver] = useState(false);
  // dragenter/dragleave bắn cả khi đi qua phần tử con: đếm để biết lúc thật sự rời vùng.
  const depth = useRef(0);
  const reset = () => { depth.current = 0; setOver(false); };
  return (
    <div className={cx("relative", className)} data-testid="file-drop-zone"
      onDragEnter={(event) => {
        if (disabled || !hasFiles(event)) return;
        event.preventDefault();
        depth.current += 1;
        setOver(true);
      }}
      onDragOver={(event) => {
        if (disabled || !hasFiles(event)) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = "copy";
      }}
      onDragLeave={() => {
        if (disabled) return;
        depth.current = Math.max(0, depth.current - 1);
        if (depth.current === 0) setOver(false);
      }}
      onDrop={(event) => {
        if (disabled || !hasFiles(event)) return;
        event.preventDefault();
        reset();
        const dropped = Array.from(event.dataTransfer.files);
        const files = dropped.filter((file) => matchesAccept(file, accept));
        // Tệp sai loại không bị bỏ đi lặng lẽ: báo loại tệp nhận được.
        if (files.length < dropped.length && accept) {
          showErrorToast(msg("shared.ui.FileDropZone.unsupportedFiles", { accept }));
        }
        if (files.length > 0) onFiles(multiple ? files : files.slice(0, 1));
      }}>
      {children}
      {over && (
        <div aria-hidden className="pointer-events-none absolute -inset-1 z-20 flex animate-fade items-center justify-center gap-2 rounded-xl border-2 border-dashed border-primary bg-primary-soft/95 text-[13px] font-semibold text-primary-ink">
          <Icon name="upload_file" size={18} />{label}
        </div>
      )}
    </div>
  );
}
