import { fetchApi, refreshSession } from "@/shared/api/client";
import { CSRF_HEADER, readCsrfToken } from "@/shared/api/csrf";
import type { ArtifactPurpose, PurposePolicy, UploadResult } from "../model/purposes";
import { msg } from "@/i18n/msg";

export function getPolicies(): Promise<PurposePolicy[]> {
  return fetchApi<PurposePolicy[]>("/api/v1/files/policies");
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
  const sendRequest = () => new Promise<XMLHttpRequest>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    current = xhr;
    xhr.open("POST", "/api/v1/files");
    xhr.withCredentials = true;
    xhr.responseType = "json";
    const csrf = readCsrfToken();
    if (csrf) xhr.setRequestHeader(CSRF_HEADER, csrf);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    xhr.onload = () => resolve(xhr);
    xhr.onerror = () => reject(new Error(msg("files.api.index.connectionLostWhileUploadingThe")));
    xhr.onabort = () => reject(new Error(msg("files.api.index.cancelled")));
    const form = new FormData();
    form.append("purpose", purpose);
    form.append("file", file);
    xhr.send(form);
  });
  const promise = (async () => {
    let xhr = await sendRequest();
    if (xhr.status === 401) {
      const outcome = await refreshSession();
      if (aborted) throw new Error(msg("files.api.index.cancelled"));
      if (outcome === "unavailable") throw new Error(msg("files.api.index.theSystemIsBusyPlease"));
      if (outcome !== "expired") xhr = await sendRequest();
    }
    if (xhr.status >= 200 && xhr.status < 300) return xhr.response as UploadResult;
    const detail = (xhr.response as { detail?: string } | null)?.detail;
    throw new Error(detail ?? msg("files.api.index.uploadFailed"));
  })();
  return {
    promise,
    abort: () => {
      aborted = true;
      (current as XMLHttpRequest | null)?.abort();
    },
  };
}
