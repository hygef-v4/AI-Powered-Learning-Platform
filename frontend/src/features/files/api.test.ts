import { afterEach, describe, expect, it, vi } from "vitest";
import { uploadFile } from "./api";

describe("uploadFile", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  });

  it("sends the decoded CSRF cookie with the authenticated multipart upload", async () => {
    document.cookie = "XSRF-TOKEN=csrf%2Btoken; path=/";

    const instances: MockXMLHttpRequest[] = [];
    class MockXMLHttpRequest {
      upload = {};
      withCredentials = false;
      status = 201;
      response = {
        fileRef: { value: "qa-file" },
        mediaType: "application/pdf",
        byteSize: 3,
        originalFileName: "qa.pdf",
        expiresAt: "2026-10-24T00:00:00Z",
      };
      responseType = "";
      open = vi.fn();
      setRequestHeader = vi.fn();
      sentBody: FormData | null = null;
      send = vi.fn((body: FormData) => {
        this.sentBody = body;
        this.onload?.();
      });
      abort = vi.fn();
      onload: (() => void) | null = null;
      onerror: (() => void) | null = null;
      onabort: (() => void) | null = null;

      constructor() {
        instances.push(this);
      }
    }
    vi.stubGlobal("XMLHttpRequest", MockXMLHttpRequest);

    const file = new File(["pdf"], "qa.pdf", { type: "application/pdf" });
    const handle = uploadFile(file, "MATERIAL", vi.fn());
    const xhr = instances[0]!;

    await expect(handle.promise).resolves.toMatchObject({ fileRef: { value: "qa-file" } });
    expect(xhr.open).toHaveBeenCalledWith("POST", "/api/v1/files");
    expect(xhr.withCredentials).toBe(true);
    expect(xhr.setRequestHeader).toHaveBeenCalledWith("X-XSRF-TOKEN", "csrf+token");
    expect(xhr.sentBody).toBeInstanceOf(FormData);
  });
});
