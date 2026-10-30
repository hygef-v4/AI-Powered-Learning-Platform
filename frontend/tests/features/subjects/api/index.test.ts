import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import {
  changeSubjectManager, changeSubjectStatus, createSubject, getSubject, getSubjectClasses, getSubjects, updateSubject,
} from "@/features/subjects/api";
import type { SubjectApiProblem } from "@/features/subjects/model/types";

function mockResponse(body: unknown, status = 200) {
  // Mỗi fetch có một body riêng; Response chỉ đọc được một lần.
  const fetchMock = vi.fn().mockImplementation(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("API client U04", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("encode query, giữ page0 và dùng cookie từ shared client", async () => {
    const fetchMock = mockResponse({ items: [], totalElements: 0 });
    await getSubjects({ q: "  A&B  ", status: "ACTIVE", page: 0, size: 20 });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/v1/admin/subjects?q=A%26B&status=ACTIVE&page=0&size=20");
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: "include" });
  });

  it("bỏ query trống, encode id và phân biệt lớp unavailable với danh sách rỗng", async () => {
    const fetchMock = mockResponse({ availability: "UNAVAILABLE", items: null });
    await getSubjects({ q: " " });
    await getSubject("id/with slash");
    await expect(getSubjectClasses("id/with slash")).resolves.toEqual({ availability: "UNAVAILABLE", items: null });
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      "/api/v1/admin/subjects",
      "/api/v1/admin/subjects/id%2Fwith%20slash",
      "/api/v1/admin/subjects/id%2Fwith%20slash/classes",
    ]);
  });

  it("gửi đúng bốn mutation và giữ version/null manager, không rò field ngoài whitelist", async () => {
    const fetchMock = mockResponse({ id: "s1", version: 2 });
    await createSubject({ code: "SWR302", name: "Name", managerId: null });
    await updateSubject("s1", { name: "Edited", description: null, version: 1, code: "EVIL" } as Parameters<typeof updateSubject>[1]);
    await changeSubjectManager("s1", { managerId: null, version: 2 });
    await changeSubjectStatus("s1", { status: "ARCHIVED", version: 3 });
    expect(fetchMock.mock.calls.map(([path, init]) => [path, init.method, JSON.parse(init.body)])).toEqual([
      ["/api/v1/admin/subjects", "POST", { code: "SWR302", name: "Name", managerId: null }],
      ["/api/v1/admin/subjects/s1", "PATCH", { name: "Edited", description: null, version: 1 }],
      ["/api/v1/admin/subjects/s1/manager", "PUT", { managerId: null, version: 2 }],
      ["/api/v1/admin/subjects/s1/status", "PUT", { status: "ARCHIVED", version: 3 }],
    ]);
    expect(new Headers(fetchMock.mock.calls[0]?.[1].headers).get("Content-Type")).toBe("application/json");
  });

  it.each([
    [503, "SUBJECT_CLASSES_UNAVAILABLE"],
  ])("giữ ProblemDetail HTTP%s cho UI", async (status, code) => {
    mockResponse({ code, detail: "Chưa thể lưu trữ" }, status);
    const error = await changeSubjectStatus("s1", { status: "ARCHIVED", version: 1 }).catch((error: unknown) => error);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).problem as SubjectApiProblem).toMatchObject({ status, code });
  });
});
