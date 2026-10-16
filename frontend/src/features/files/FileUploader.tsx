"use client";

import { useState } from "react";
import { acceptAttribute, type ArtifactPurpose, type UploadResult } from "./purposes";
import { useFileUpload } from "./useFileUpload";

type Props = {
  purpose: ArtifactPurpose;
  label?: string;
  onUploaded: (result: UploadResult) => void;
};

/** Chọn hoặc kéo thả tệp, kiểm sơ bộ theo giới hạn hiện hành, có tiến trình và nút Hủy. */
export function FileUploader({ purpose, label = "Chọn tệp", onUploaded }: Props) {
  const { policy, state, start, cancel } = useFileUpload(purpose);
  const [dragging, setDragging] = useState(false);

  async function handle(file: File | undefined) {
    if (!file) return;
    const result = await start(file);
    if (result) onUploaded(result);
  }

  return (
    <div
      className={`rounded border-2 border-dashed p-4 ${dragging ? "border-blue-500" : "border-slate-300"}`}
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
      <label className="cursor-pointer text-blue-700">
        {label}
        <input
          type="file"
          className="sr-only"
          disabled={!policy || state.status === "uploading"}
          accept={policy ? acceptAttribute(policy) : undefined}
          onChange={(e) => void handle(e.target.files?.[0])}
        />
      </label>
      {policy && (
        <p className="mt-1 text-xs text-slate-500">
          {policy.allowedTypes.join(", ")}, tối đa {policy.maxSizeMb} MB
        </p>
      )}
      {state.status === "uploading" && (
        <div className="mt-2 flex items-center gap-2 text-sm">
          <progress max={100} value={state.percent} />
          <span>{state.percent}%</span>
          <button type="button" className="underline" onClick={cancel}>
            Hủy
          </button>
        </div>
      )}
      {state.status === "error" && (
        <p role="alert" className="mt-2 text-sm text-red-700">
          {state.message}
        </p>
      )}
      {state.status === "done" && <p className="mt-2 text-sm text-green-700">Đã tải lên {state.result.originalFileName}</p>}
    </div>
  );
}
