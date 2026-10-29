import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "./client";

describe("apiFetch", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("gửi cookie và trả JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));
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
});
