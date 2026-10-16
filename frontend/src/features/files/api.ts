import { apiFetch } from "@/shared/api/client";
import type { ArtifactPurpose, PurposePolicy, UploadResult } from "./purposes";

export function getPolicies(): Promise<PurposePolicy[]> {
  return apiFetch<PurposePolicy[]>("/api/v1/files/policies");
}

export type UploadHandle = {
  promise: Promise<UploadResult>;
  abort: () => void;
};

/** Upload multipart có tiến trình và hủy được (XMLHttpRequest vì fetch chưa báo tiến trình gửi). */
export function uploadFile(file: File, purpose: ArtifactPurpose, onProgress: (percent: number) => void): UploadHandle {
  const xhr = new XMLHttpRequest();
  const promise = new Promise<UploadResult>((resolve, reject) => {
    xhr.open("POST", "/api/v1/files");
    xhr.withCredentials = true;
    xhr.responseType = "json";
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.response as UploadResult);
      } else {
        const detail = (xhr.response as { detail?: string } | null)?.detail;
        reject(new Error(detail ?? "Tải tệp lên không thành công"));
      }
    };
    xhr.onerror = () => reject(new Error("Mất kết nối khi tải tệp lên"));
    xhr.onabort = () => reject(new Error("Đã hủy"));
    const form = new FormData();
    form.append("purpose", purpose);
    form.append("file", file);
    xhr.send(form);
  });
  return { promise, abort: () => xhr.abort() };
}
