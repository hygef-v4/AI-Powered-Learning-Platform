import { afterEach, describe, expect, it, vi } from "vitest";
import { uploadFile } from "./api";

/** XMLHttpRequest giả: ghi header đã đặt, trả lần lượt các status đã hẹn. */
function fakeXhr(statuses: number[]) {
  const sent: { headers: Record<string, string> }[] = [];
  class FakeXhr {
    status = 0;
    response: unknown = null;
    responseType = "";
    withCredentials = false;
    upload: { onprogress: ((e: ProgressEvent) => void) | null } = { onprogress: null };
    onload: (() => void) | null = null;
    onerror: (() => void) | null = null;
    onabort: (() => void) | null = null;
    private headers: Record<string, string> = {};
    open() {}
    setRequestHeader(name: string, value: string) {
      this.headers[name] = value;
    }
    send() {
      sent.push({ headers: this.headers });
      const status = statuses[sent.length - 1] ?? 500;
      this.status = status;
      this.response = status === 201 ? { fileRef: { value: "ref-1" } } : { detail: `HTTP ${status}` };
      queueMicrotask(() => this.onload?.());
    }
    abort() {
      this.onabort?.();
    }
  }
  vi.stubGlobal("XMLHttpRequest", FakeXhr);
  return sent;
}

describe("uploadFile", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; max-age=0";
  });

  it("gửi header CSRF đọc từ cookie", async () => {
    document.cookie = "XSRF-TOKEN=tok%3D1";
    const sent = fakeXhr([201]);

    const result = await uploadFile(new File(["x"], "a.png", { type: "image/png" }), "DOCUMENT_IMAGE", () => {}).promise;

    expect(result).toEqual({ fileRef: { value: "ref-1" } });
    expect(sent[0]?.headers["X-XSRF-TOKEN"]).toBe("tok=1");
  });

  it("gặp 401 thì refresh phiên một lần rồi gửi lại", async () => {
    const sent = fakeXhr([401, 201]);
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(uploadFile(new File(["x"], "a.png"), "DOCUMENT_IMAGE", () => {}).promise)
      .resolves.toEqual({ fileRef: { value: "ref-1" } });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/v1/auth/refresh");
    expect(sent).toHaveLength(2);
  });

  it("lỗi khác thì báo detail của server", async () => {
    fakeXhr([415]);

    await expect(uploadFile(new File(["x"], "a.gif"), "DOCUMENT_IMAGE", () => {}).promise).rejects.toThrow("HTTP 415");
  });
});
