"use client";

import { useRef, useState } from "react";
import { Button } from "@/shared/ui/Button";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { acceptAttribute, type ArtifactPurpose, type UploadResult } from "./purposes";
import { useFileUpload } from "./useFileUpload";

type Props = {
  purpose: ArtifactPurpose;
  label?: string;
  onUploaded: (result: UploadResult) => void;
};

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace(".", ",")} MB`;
}

/** Chọn hoặc kéo thả tệp, kiểm sơ bộ theo giới hạn hiện hành, có tiến trình và nút Hủy. */
export function FileUploader({ purpose, label = "Chọn tệp", onUploaded }: Props) {
  const { policy, state, start, cancel } = useFileUpload(purpose);
  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState<{ name: string; size: number } | null>(null);
  const input = useRef<HTMLInputElement>(null);
  const uploading = state.status === "uploading";

  async function handle(picked: File | undefined) {
    if (!picked) return;
    setFile({ name: picked.name, size: picked.size });
    const result = await start(picked);
    if (result) onUploaded(result);
  }

  const picker = (
    <input
      ref={input}
      type="file"
      className="sr-only"
      aria-label={label}
      disabled={!policy || uploading}
      accept={policy ? acceptAttribute(policy) : undefined}
      onChange={(e) => {
        void handle(e.target.files?.[0]);
        e.target.value = "";
      }}
    />
  );

  return (
    <div className="flex flex-col gap-2">
      {file && state.status !== "idle" ? (
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex h-[38px] w-[360px] max-w-full items-center gap-2 rounded-lg border border-line-strong bg-surface px-3">
            <Icon
              name={state.status === "done" ? "check_circle" : "description"}
              size={18}
              className={state.status === "done" ? "text-success" : "text-muted"}
            />
            <span className="min-w-0 flex-1 truncate text-[13px] text-ink">{file.name}</span>
            <span className="text-xs text-muted">{uploading ? `${state.percent}%` : formatSize(file.size)}</span>
          </div>
          {uploading ? (
            <Button icon="close" onClick={cancel}>
              Hủy
            </Button>
          ) : (
            <Button icon="folder_open" disabled={!policy} onClick={() => input.current?.click()}>
              Chọn tệp khác
            </Button>
          )}
          {picker}
        </div>
      ) : (
        <div
          className={cx(
            "flex flex-col items-center gap-2 rounded-xl border border-dashed bg-surface px-4 py-5 text-center",
            dragging ? "border-primary bg-primary-soft" : "border-line-strong",
          )}
          onDragOver={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragging(false);
            void handle(e.dataTransfer.files[0]);
          }}
        >
          <Icon name="upload_file" size={28} className="text-muted" />
          <p className="text-[13px] text-body">Kéo thả tệp vào đây hoặc</p>
          <Button icon="folder_open" disabled={!policy} onClick={() => input.current?.click()}>
            {label}
          </Button>
          {picker}
        </div>
      )}
      {uploading && (
        <div className="h-1.5 w-[360px] max-w-full overflow-hidden rounded-full bg-line-soft">
          <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${state.percent}%` }} />
        </div>
      )}
      {policy && (
        <p className="text-[11px] text-muted">
          {policy.allowedTypes.join(", ")}, tối đa {policy.maxSizeMb} MB
        </p>
      )}
      {state.status === "error" && (
        <p role="alert" className="flex items-center gap-1 text-[11px] text-danger">
          <Icon name="error" size={14} />
          {state.message}
        </p>
      )}
    </div>
  );
}
