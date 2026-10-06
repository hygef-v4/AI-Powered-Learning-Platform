"use client";

import { useCallback, useRef, useState } from "react";
import type { ArtifactPurpose } from "./purposes";

export type UploadedFileRef = {
  fileRef: string;
  expiresAt: string;
  mediaType: string;
  byteSize: number;
  originalFileName: string;
};

export type UploadStatus = "idle" | "uploading" | "done" | "error" | "cancelled";

const GENERIC_ERROR = "Tải lên thất bại, vui lòng thử lại.";

/**
 * `POST /api/v1/files` multipart bằng XHR để có tiến trình và hủy được (Bước 24). Hủy giữa chừng thì backend không
 * giữ lại gì. Lỗi backend hiện nguyên thông điệp an toàn (`title` của problem-details).
 */
export function useFileUpload(purpose: ArtifactPurpose) {
  const [status, setStatus] = useState<UploadStatus>("idle");
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | undefined>();
  const xhrRef = useRef<XMLHttpRequest | null>(null);

  const upload = useCallback(
    (file: File) =>
      new Promise<UploadedFileRef | null>((resolve) => {
        const form = new FormData();
        form.append("purpose", purpose);
        form.append("file", file);

        const xhr = new XMLHttpRequest();
        xhrRef.current = xhr;
        xhr.open("POST", "/api/v1/files");
        xhr.withCredentials = true;
        xhr.setRequestHeader("Accept", "application/json");

        xhr.upload.onprogress = (e) => {
          if (e.lengthComputable) setProgress(Math.round((e.loaded / e.total) * 100));
        };
        xhr.onload = () => {
          xhrRef.current = null;
          if (xhr.status === 201) {
            setStatus("done");
            setProgress(100);
            resolve(JSON.parse(xhr.responseText) as UploadedFileRef);
            return;
          }
          setStatus("error");
          setError(readTitle(xhr.responseText));
          resolve(null);
        };
        xhr.onerror = () => {
          xhrRef.current = null;
          setStatus("error");
          setError(GENERIC_ERROR);
          resolve(null);
        };
        xhr.onabort = () => {
          xhrRef.current = null;
          setStatus("cancelled");
          resolve(null);
        };

        setStatus("uploading");
        setProgress(0);
        setError(undefined);
        xhr.send(form);
      }),
    [purpose],
  );

  const cancel = useCallback(() => xhrRef.current?.abort(), []);

  return { upload, cancel, status, progress, error };
}

function readTitle(body: string): string {
  try {
    const problem = JSON.parse(body) as { title?: unknown };
    return typeof problem.title === "string" && problem.title ? problem.title : GENERIC_ERROR;
  } catch {
    return GENERIC_ERROR;
  }
}
