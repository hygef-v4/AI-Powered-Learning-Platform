import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { uploadFile } from "@/features/files/api";

class UploadRequest {
  static latest: UploadRequest;
  status = 201;
  response: unknown = {
    fileRef: { value: "qa-file" }, mediaType: "application/pdf", byteSize: 3,
    originalFileName: "qa.pdf", expiresAt: "2026-10-24T00:00:00Z",
  };
  withCredentials = false;
  responseType = "";
  upload = { onprogress: null as ((event: { lengthComputable: boolean; loaded: number; total: number }) => void) | null };
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onabort: (() => void) | null = null;
  open = vi.fn();
  setRequestHeader = vi.fn<(name: string, value: string) => void>();
  send = vi.fn<(body: FormData) => void>();
  abort = vi.fn(() => this.onabort?.());
  constructor() { UploadRequest.latest = this; }
}

describe("multipart upload", () => {
  beforeEach(() => {
    document.cookie = "XSRF-TOKEN=; max-age=0";
    vi.stubGlobal("XMLHttpRequest", UploadRequest);
  });
  afterEach(() => { document.cookie = "XSRF-TOKEN=; max-age=0"; vi.unstubAllGlobals(); });

  it("refreshes an expired session once and resends the upload", async () => {
    const refresh = vi.fn(async () => new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", refresh);
    const upload = uploadFile(new File(["pdf"], "qa.pdf"), "MATERIAL", vi.fn());
    const first = UploadRequest.latest;
    first.status = 401; first.response = { detail: "Bạn cần đăng nhập" }; first.onload?.();
    await vi.waitFor(() => expect(UploadRequest.latest).not.toBe(first));
    const second = UploadRequest.latest;
    expect(refresh).toHaveBeenCalledWith("/api/v1/auth/refresh", expect.objectContaining({ method: "POST" }));
    second.onload?.();
    await expect(upload.promise).resolves.toEqual(second.response);
    expect(first.send).toHaveBeenCalledOnce();
    expect(second.send).toHaveBeenCalledOnce();
  });
});
