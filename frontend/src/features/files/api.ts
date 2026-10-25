import { apiFetch, CSRF_HEADER, csrfToken, refreshSession } from "@/shared/api/client";
import type { ArtifactPurpose, PurposePolicy, UploadResult } from "./purposes";

export function getPolicies(): Promise<PurposePolicy[]> {
  return apiFetch<PurposePolicy[]>("/api/v1/files/policies");
}

export type UploadHandle = {
  promise: Promise<UploadResult>;
  abort: () => void;
};

/**
 * Upload multipart có tiến trình và hủy được (XMLHttpRequest vì fetch chưa báo tiến trình gửi). Không qua apiFetch nên
 * tự gửi header CSRF và tự refresh phiên một lần khi gặp 401, như apiFetch.
 */
export function uploadFile(file: File, purpose: ArtifactPurpose, onProgress: (percent: number) => void): UploadHandle {
  let xhr: XMLHttpRequest | null = null;
  let aborted = false;

  function send(): Promise<XMLHttpRequest> {
    return new Promise((resolve, reject) => {
      const request = new XMLHttpRequest();
      xhr = request;
      request.open("POST", "/api/v1/files");
      request.withCredentials = true;
      request.responseType = "json";
      const csrf = csrfToken();
      if (csrf) request.setRequestHeader(CSRF_HEADER, csrf);
      request.upload.onprogress = (event) => {
        if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
      };
      request.onload = () => resolve(request);
      request.onerror = () => reject(new Error("Mất kết nối khi tải tệp lên"));
      request.onabort = () => reject(new Error("Đã hủy"));
      const form = new FormData();
      form.append("purpose", purpose);
      form.append("file", file);
      request.send(form);
    });
  }

  const promise = (async () => {
    let response = await send();
    if (response.status === 401 && !aborted) {
      const outcome = await refreshSession();
      if (aborted) throw new Error("Đã hủy");
      if (outcome === "refreshed" || outcome === "concurrent") response = await send();
    }
    if (response.status >= 200 && response.status < 300) return response.response as UploadResult;
    const detail = (response.response as { detail?: string } | null)?.detail;
    throw new Error(detail ?? "Tải tệp lên không thành công");
  })();
  return {
    promise,
    abort: () => {
      aborted = true;
      xhr?.abort();
    },
  };
}
