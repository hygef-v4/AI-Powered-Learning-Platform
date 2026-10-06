"use client";

import { useRef, useState, type DragEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { PURPOSE_RULES, precheckFile, type ArtifactPurpose } from "./purposes";
import { useFileUpload, type UploadedFileRef } from "./useFileUpload";

type FileUploaderProps = {
  purpose: ArtifactPurpose;
  onUploaded: (file: UploadedFileRef) => void;
  label?: string;
  disabled?: boolean;
};

/** Chọn hoặc kéo thả một tệp; kiểm sơ bộ đuôi và dung lượng; thanh tiến trình; nút Hủy (frontend-components U03). */
export function FileUploader({ purpose, onUploaded, label = "Chọn tệp", disabled = false }: FileUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [localError, setLocalError] = useState<string | undefined>();
  const [dragging, setDragging] = useState(false);
  const { upload, cancel, status, progress, error } = useFileUpload(purpose);
  const uploading = status === "uploading";

  const start = async (file: File | undefined) => {
    if (!file) return;
    const problem = precheckFile(file, purpose);
    setLocalError(problem ?? undefined);
    if (problem) return;
    const result = await upload(file);
    if (result) onUploaded(result);
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragging(false);
    if (!disabled && !uploading) void start(e.dataTransfer.files[0]);
  };

  return (
    <div className="flex flex-col gap-2">
      <div
        data-testid="file-uploader-dropzone"
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={`flex flex-col items-center gap-2 rounded-md border-2 border-dashed p-4 text-sm ${
          dragging ? "border-brand bg-blue-50" : "border-slate-300"
        }`}
      >
        <p className="text-slate-600">Kéo thả tệp vào đây hoặc</p>
        <input
          ref={inputRef}
          type="file"
          hidden
          accept={PURPOSE_RULES[purpose].accept}
          data-testid="file-uploader-input"
          onChange={(e) => {
            void start(e.target.files?.[0]);
            e.target.value = "";
          }}
        />
        <Button
          variant="secondary"
          disabled={disabled || uploading}
          onClick={() => inputRef.current?.click()}
          data-testid="file-uploader-select-button"
        >
          {label}
        </Button>
      </div>

      {uploading && (
        <div className="flex items-center gap-2" data-testid="file-uploader-progress">
          <progress className="h-2 flex-1" value={progress} max={100} aria-label="Tiến trình tải lên" />
          <span className="w-10 text-right text-xs">{progress}%</span>
          <Button variant="ghost" onClick={cancel} data-testid="file-uploader-cancel-button">
            Hủy
          </Button>
        </div>
      )}
      {(localError ?? error) && <Alert tone="error">{localError ?? error}</Alert>}
    </div>
  );
}
