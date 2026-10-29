import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { uploadFile } from "./api";

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

  it("sends the decoded CSRF cookie with credentials without overriding the multipart boundary", async () => {
    document.cookie = "XSRF-TOKEN=encoded%2Btoken";
    const file = new File(["pdf"], "qa.pdf", { type: "application/pdf" });
    const progress = vi.fn();
    const upload = uploadFile(file, "MATERIAL", progress);
    const xhr = UploadRequest.latest;
    expect(xhr.open).toHaveBeenCalledWith("POST", "/api/v1/files");
    expect(xhr.setRequestHeader).toHaveBeenCalledWith("X-XSRF-TOKEN", "encoded+token");
    expect(xhr.withCredentials).toBe(true);
    expect(xhr.setRequestHeader.mock.calls.some(([name]) => name.toLowerCase() === "content-type")).toBe(false);
    const form = xhr.send.mock.calls[0]![0];
    expect(form.get("purpose")).toBe("MATERIAL");
    expect(form.get("file")).toBe(file);
    xhr.upload.onprogress?.({ lengthComputable: true, loaded: 1, total: 2 });
    expect(progress).toHaveBeenCalledWith(50);
    xhr.onload?.();
    await expect(upload.promise).resolves.toEqual(xhr.response);
  });

  it("does not invent a CSRF token when no cookie exists", async () => {
    const upload = uploadFile(new File(["pdf"], "qa.pdf"), "MATERIAL", vi.fn());
    const xhr = UploadRequest.latest;
    expect(xhr.setRequestHeader).not.toHaveBeenCalled();
    xhr.status = 403; xhr.response = { detail: "Phiên làm việc không hợp lệ" }; xhr.onload?.();
    await expect(upload.promise).rejects.toThrow("Phiên làm việc không hợp lệ");
    expect(xhr.send).toHaveBeenCalledOnce();
  });

  it("keeps cancellation terminal and reports network failure without replaying the upload", async () => {
    const upload = uploadFile(new File(["pdf"], "qa.pdf"), "MATERIAL", vi.fn());
    const xhr = UploadRequest.latest;
    upload.abort();
    await expect(upload.promise).rejects.toThrow("Đã hủy");
    expect(xhr.send).toHaveBeenCalledOnce();
    const failed = uploadFile(new File(["pdf"], "qa.pdf"), "MATERIAL", vi.fn());
    UploadRequest.latest.onerror?.();
    await expect(failed.promise).rejects.toThrow("Mất kết nối");
    expect(UploadRequest.latest.send).toHaveBeenCalledOnce();
  });
});
