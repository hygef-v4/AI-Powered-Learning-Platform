import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch, setUnauthenticatedHandler } from "./client";

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status });
}

describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    setUnauthenticatedHandler(null);
    document.cookie = "XSRF-TOKEN=; max-age=0";
  });

  it("gửi cookie và trả JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch<{ ok: boolean }>("/api/v1/x")).resolves.toEqual({ ok: true });
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: "include" });
  });

  it("ném ApiError kèm problem detail khi lỗi", async () => {
    const body = JSON.stringify({ title: "Conflict", detail: "Version lệch" });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(body, { status: 409 })));

    const error = await apiFetch("/api/v1/x").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).problem).toMatchObject({ status: 409, detail: "Version lệch" });
  });

  it("gửi header CSRF cho request đổi trạng thái, không gửi cho GET", async () => {
    document.cookie = "XSRF-TOKEN=abc";
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(json({})));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/api/v1/x", { method: "PATCH", body: "{}" });
    await apiFetch("/api/v1/x");

    expect(new Headers(fetchMock.mock.calls[0]?.[1].headers).get("X-XSRF-TOKEN")).toBe("abc");
    expect(new Headers(fetchMock.mock.calls[1]?.[1].headers).get("X-XSRF-TOKEN")).toBeNull();
  });

  it("gặp 401 thì refresh một lần rồi gửi lại", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(json({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/api/v1/me/profile")).resolves.toEqual({ ok: true });
    expect(fetchMock.mock.calls[1]?.[0]).toBe("/api/v1/auth/refresh");
  });

  it("refresh lỗi thì gọi handler hết phiên và ném 401", async () => {
    const handler = vi.fn();
    setUnauthenticatedHandler(handler);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() => Promise.resolve(new Response(null, { status: 401 }))),
    );

    const error = await apiFetch("/api/v1/me/profile").catch((e: unknown) => e);
    expect((error as ApiError).problem.status).toBe(401);
    expect(handler).toHaveBeenCalledOnce();
  });

  it("không refresh cho endpoint đăng nhập", async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ code: "AUTH_FAILED" }, 401));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/api/v1/auth/login", { method: "POST", body: "{}" }).catch(() => undefined);
    expect(fetchMock).toHaveBeenCalledOnce();
  });
});
