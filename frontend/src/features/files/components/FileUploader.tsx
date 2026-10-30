"use client";

import { useRef, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { acceptAttribute, type ArtifactPurpose, type UploadResult } from "../model/purposes";
import { useFileUpload } from "../hooks/useFileUpload";

type Props = {
  purpose: ArtifactPurpose;
  label?: string;
  /** Tên tệp đang có (khi sửa); chọn tệp mới thì thay. */
  currentName?: string | null;
  onUploaded: (result: UploadResult) => void;
};

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace(".", ",")} MB`;
}

/**
 * Ô tải tệp một dòng, cùng kiểu ô chọn tệp CSV ở trang Tài khoản: ô tên tệp rồi nút Chọn tệp (kéo thả vào ô cũng được);
 * đang tải thì hiện phần trăm và nút Hủy. Kiểm sơ bộ theo giới hạn hiện hành.
 */
export function FileUploader({ purpose, label = "Chọn tệp", currentName, onUploaded }: Props) {
  const { policy, state, start, cancel } = useFileUpload(purpose);
  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState<{ name: string; size: number } | null>(null);
  const input = useRef<HTMLInputElement>(null);
  const uploading = state.status === "uploading";
  const shown = file && state.status !== "idle" ? file : null;

  async function handle(picked: File | undefined) {
    if (!picked) return;
    setFile({ name: picked.name, size: picked.size });
    const result = await start(picked);
    if (result) onUploaded(result);
  }

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex min-w-0 items-center gap-2"
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => { e.preventDefault(); setDragging(false); void handle(e.dataTransfer.files[0]); }}>
        <div className={cx("relative flex h-10 min-w-0 flex-1 items-center gap-2 overflow-hidden rounded-lg border bg-surface px-3",
          dragging ? "border-primary bg-primary-soft" : "border-field")}>
          <Icon name={state.status === "done" ? "check_circle" : "description"} size={18}
            className={cx("shrink-0", state.status === "done" ? "text-success" : "text-muted")} />
          <span className={cx("min-w-0 flex-1 truncate text-[13px]", shown || currentName ? "text-ink" : "text-muted")}>
            {shown?.name ?? currentName ?? "Chưa chọn tệp"}
          </span>
          {shown && <span className="shrink-0 text-xs text-muted">{uploading ? `${state.percent}%` : formatSize(shown.size)}</span>}
          {uploading && <span aria-hidden="true" className="absolute bottom-0 left-0 h-0.5 bg-primary transition-all" style={{ width: `${state.percent}%` }} />}
        </div>
        {uploading
          ? <Button icon="close" onClick={cancel}>Hủy</Button>
          : <Button icon="folder_open" disabled={!policy} onClick={() => input.current?.click()}>{label}</Button>}
        <input ref={input} type="file" className="sr-only" aria-label={label} disabled={!policy || uploading}
          accept={policy ? acceptAttribute(policy) : undefined}
          onChange={(e) => { void handle(e.target.files?.[0]); e.target.value = ""; }} />
      </div>
      {state.status === "error" && (
        <p role="alert" className="flex items-center gap-1 text-[11px] text-danger">
          <Icon name="error" size={14} />
          {state.message}
        </p>
      )}
    </div>
  );
}
