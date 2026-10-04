import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "./client";

function mockFetch(response: Response) {
  const fn = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fn);
  return fn;
}

afterEach(() => vi.unstubAllGlobals());

describe("apiFetch", () => {
  it("gửi cookie và body JSON", async () => {
    const fn = mockFetch(Response.json({ ok: true }));
    await apiFetch("/things", { method: "POST", body: { a: 1 }, idempotencyKey: "k1" });

    const [url, init] = fn.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/things");
    expect(init.credentials).toBe("include");
    expect(init.body).toBe('{"a":1}');
    expect(new Headers(init.headers).get("Idempotency-Key")).toBe("k1");
  });

  it("đọc problem-details khi lỗi", async () => {
    mockFetch(
      new Response(JSON.stringify({ title: "Dữ liệu không hợp lệ.", code: "VALIDATION_FAILED" }), {
        status: 400,
        headers: { "Content-Type": "application/problem+json" },
      }),
    );

    const error = await apiFetch("/things").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe("VALIDATION_FAILED");
    expect((error as ApiError).message).toBe("Dữ liệu không hợp lệ.");
  });

  it("trả undefined với 204", async () => {
    mockFetch(new Response(null, { status: 204 }));
    await expect(apiFetch("/things", { method: "DELETE" })).resolves.toBeUndefined();
  });
});
