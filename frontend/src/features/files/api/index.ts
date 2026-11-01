import { apiFetch, refreshSession } from "@/shared/api/client";
import { CSRF_HEADER, csrfToken } from "@/shared/api/csrf";
import type { ArtifactPurpose, PurposePolicy, UploadResult } from "../model/purposes";

export function getPolicies(): Promise<PurposePolicy[]> {
  return apiFetch<PurposePolicy[]>("/api/v1/files/policies");
}

export type UploadHandle = {
  promise: Promise<UploadResult>;
  abort: () => void;
};

/**
 * Upload multipart có tiến trình và hủy được (XMLHttpRequest vì fetch chưa báo tiến trình gửi). Access token hết hạn
 * (401) thì làm mới phiên rồi gửi lại một lần như `apiFetch`; phiên hết hẳn thì báo như các request khác.
 */
export function uploadFile(file: File, purpose: ArtifactPurpose, onProgress: (percent: number) => void): UploadHandle {
  let current: XMLHttpRequest | null = null;
  let aborted = false;
  const send = () => new Promise<XMLHttpRequest>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    current = xhr;
    xhr.open("POST", "/api/v1/files");
    xhr.withCredentials = true;
    xhr.responseType = "json";
    const csrf = csrfToken();
    if (csrf) xhr.setRequestHeader(CSRF_HEADER, csrf);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    xhr.onload = () => resolve(xhr);
    xhr.onerror = () => reject(new Error("Mất kết nối khi tải tệp lên"));
    xhr.onabort = () => reject(new Error("Đã hủy"));
    const form = new FormData();
    form.append("purpose", purpose);
    form.append("file", file);
    xhr.send(form);
  });
  const promise = (async () => {
    let xhr = await send();
    if (xhr.status === 401) {
      const outcome = await refreshSession();
      if (aborted) throw new Error("Đã hủy");
      if (outcome === "unavailable") throw new Error("Hệ thống tạm bận, vui lòng thử lại sau");
      if (outcome !== "expired") xhr = await send();
    }
    if (xhr.status >= 200 && xhr.status < 300) return xhr.response as UploadResult;
    const detail = (xhr.response as { detail?: string } | null)?.detail;
    throw new Error(detail ?? "Tải tệp lên không thành công");
  })();
  return {
    promise,
    abort: () => {
      aborted = true;
      (current as XMLHttpRequest | null)?.abort();
    },
  };
}
