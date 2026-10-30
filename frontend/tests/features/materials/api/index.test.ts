import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "@/shared/api/client";
import { materialApi, materialError, validYoutube } from "@/features/materials/api";
import type { Material } from "@/features/materials/model/types";
vi.mock("@/shared/api/client", async importOriginal => ({ ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: vi.fn() }));
beforeEach(() => { vi.mocked(apiFetch).mockReset(); });
describe("single video validation", () => {
  it.each(["https://youtu.be/abcdefghijk", "https://youtube.com/watch?v=abcdefghijk", "https://www.youtube.com/watch?v=abcdefghijk&t=20"])("accepts %s", url => expect(validYoutube(url)).toBe(true));
  it.each(["javascript:alert(1)", "https://youtube.com.evil.test/watch?v=abcdefghijk", "https://youtube.com/watch?v=abcdefghijk&list=x", "https://youtu.be/abcdefghijk?list=x", "https://user@youtube.com/watch?v=abcdefghijk", "https://youtube.com/watch?v=abcdefghijk&v=abcdefghijk", "http://youtu.be/abcdefghijk", "https://youtu.be/short"])("rejects %s", url => expect(validYoutube(url)).toBe(false));
});
it("replacement carries the signed file reference and optimistic version", async () => {
  await materialApi.replace({ id: "id", version: 3 } as Material, { fileRef: "signed-reference" });
  expect(apiFetch).toHaveBeenCalledWith("/api/v1/materials/id/versions", { method: "POST", body: JSON.stringify({ version: 3, source: { fileRef: "signed-reference" } }) });
});
it("reorder submits all expected versions in requested order", async () => {
  await materialApi.reorder("/scope/materials", [{ id: "b", version: 4 }, { id: "a", version: 2 }] as Material[]);
  expect(apiFetch).toHaveBeenCalledWith("/scope/materials:reorder", { method: "PUT", body: JSON.stringify({ items: [{ id: "b", version: 4 }, { id: "a", version: 2 }] }) });
});
it("token request preserves class and historical version", async () => {
  await materialApi.token("id", "class", 1);
  expect(apiFetch).toHaveBeenCalledWith("/api/v1/materials/id/download-token?classId=class&versionNo=1", { method: "POST" });
});
it("conflict instructs reloading instead of silently retrying write", () => {
  expect(materialError(new ApiError({ status: 409 }))).toContain("Tải lại");
  expect(materialError(new ApiError({ status: 404 }))).toContain("không có quyền");
});
