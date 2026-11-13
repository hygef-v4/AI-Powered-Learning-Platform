"use client";

import { useState, type ReactNode } from "react";
import { Icon, IconButton } from "@/shared/ui";

type Props = {
  file: File | null;
  onChange: (file: File | null) => void;
  disabled?: boolean;
  /** Chữ trên ô kéo thả, cũng là tên của ô chọn tệp. */
  label?: string;
  accept?: string;
  testId?: string;
  /** Trạng thái hiện cạnh tên tệp đã chọn (đang tải, đã đọc, lỗi). */
  status?: ReactNode;
  /** Chọn được nhiều tệp (ảnh đề cũ, BR-U07-87): {@code onFilesChange} nhận cả danh sách theo thứ tự chọn. */
  files?: File[];
  onFilesChange?: (files: File[]) => void;
};

/**
 * Ô tải đề mẫu: kéo thả hoặc bấm để chọn một tệp; chọn xong chỉ hiện tên tệp và nút bỏ. Dùng chung cho Tạo bài tập (đề mẫu
 * DOCX) và Soạn bằng AI (định dạng đề, yêu cầu 2026-11-05).
 */
export function TemplateFileField({ file, onChange, disabled, label = "Upload đề mẫu", accept = ".docx", testId = "create-docx", status,
  files, onFilesChange }: Props) {
  const [over, setOver] = useState(false);
  const multiple = !!onFilesChange;
  const pick = (list: FileList | null | undefined) => {
    const chosen = Array.from(list ?? []);
    if (multiple) onFilesChange(chosen);
    else onChange(chosen[0] ?? null);
  };
  const extra = multiple && files && files.length > 1 ? files.length - 1 : 0;
  if (file) {
    return (
      <div className="flex animate-fade items-center gap-3 rounded-[10px] border border-line bg-sunken px-3.5 py-3 text-[13.5px]" data-testid="docx-file">
        <Icon name="description" size={18} className="text-primary-ink" />
        <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={file.name}>{file.name}</span>
        {extra > 0 && <span className="shrink-0 text-xs text-muted">+{extra} tệp</span>}
        {status}
        <IconButton icon="close" label="Bỏ tệp" size={28} disabled={disabled}
          onClick={() => (multiple ? onFilesChange([]) : onChange(null))} />
      </div>
    );
  }
  return (
    <label
      onDragOver={(event) => { event.preventDefault(); setOver(true); }}
      onDragLeave={() => setOver(false)}
      onDrop={(event) => {
        event.preventDefault();
        setOver(false);
        if (!disabled && event.dataTransfer.files.length > 0) pick(event.dataTransfer.files);
      }}
      className={`flex cursor-pointer items-center gap-3 rounded-[10px] border-[1.5px] border-dashed px-3.5 py-3.5 text-[13.5px] font-semibold transition-colors duration-200 ${
        over ? "border-primary bg-primary-soft text-ink" : "border-line-strong text-body hover:border-primary hover:bg-sunken"
      }`}
    >
      <Icon name="upload_file" size={18} className="text-primary-ink" />
      {label}
      <input type="file" accept={accept} multiple={multiple} className="sr-only" aria-label={label} data-testid={testId}
        disabled={disabled} onChange={(event) => { pick(event.target.files); event.target.value = ""; }} />
    </label>
  );
}
