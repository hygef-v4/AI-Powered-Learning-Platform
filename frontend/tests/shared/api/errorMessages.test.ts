import { afterEach, describe, expect, it, vi } from "vitest";

/** `LOCALE` đọc cookie lúc nạp module, nên mỗi ngôn ngữ nạp lại module sau khi đặt cookie. */
async function loadFor(locale: "en" | "vi") {
  document.cookie = `locale=${locale}; path=/`;
  vi.resetModules();
  return import("@/shared/api/errorMessages");
}

afterEach(() => {
  document.cookie = "locale=vi; path=/";
  vi.resetModules();
});

describe("describeApiProblem (English UI)", () => {
  it("translates known codes instead of showing the Vietnamese server text", async () => {
    const { describeApiProblem } = await loadFor("en");
    expect(describeApiProblem({ status: 415, code: "FILE_TYPE_NOT_ALLOWED", detail: "Loại tệp không được phép" }))
      .toBe("This file type is not allowed.");
    expect(describeApiProblem({ status: 400, code: "MATERIAL_YOUTUBE_INVALID", detail: "Chỉ nhận link của một video YouTube" }))
      .toBe("Only a link to a single YouTube video is accepted.");
  });

  it("keeps the size limit from the server text", async () => {
    const { describeApiProblem } = await loadFor("en");
    expect(describeApiProblem({ status: 413, code: "FILE_TOO_LARGE", detail: "Tệp vượt 50 MB" })).toBe("The file exceeds 50 MB.");
    expect(describeApiProblem({ status: 413 })).toBe("The file is larger than allowed.");
  });

  it("falls back to a sentence for the status or for any *_NOT_FOUND code", async () => {
    const { describeApiProblem } = await loadFor("en");
    expect(describeApiProblem({ status: 404, code: "ANNOUNCEMENT_NOT_FOUND", detail: "Không tìm thấy" }))
      .toBe("Not found or you do not have access.");
    expect(describeApiProblem({ status: 409, code: "SOMETHING_NEW", detail: "Đã thay đổi" }))
      .toBe("The data has just changed. Reload the page and try again.");
    expect(describeApiProblem({ status: 422, code: "UNKNOWN", detail: "Lỗi" })).toBe("Could not complete the request. Please try again.");
    expect(describeApiProblem({ status: 502 })).toBe("The system is busy, please try again later.");
  });
});

describe("describeApiProblem (Vietnamese UI)", () => {
  it("keeps the server's Vietnamese text, which is more specific", async () => {
    const { describeApiProblem } = await loadFor("vi");
    expect(describeApiProblem({ status: 415, code: "FILE_TYPE_NOT_ALLOWED", detail: "Loại tệp không được phép" }))
      .toBe("Loại tệp không được phép");
  });

  it("uses a Vietnamese sentence when the server sent no text", async () => {
    const { describeApiProblem } = await loadFor("vi");
    expect(describeApiProblem({ status: 413 })).toBe("Tệp vượt dung lượng cho phép.");
  });
});

describe("fetchApi error text", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("puts the translated text in detail and keeps the server text in serverDetail", async () => {
    document.cookie = "locale=en; path=/";
    vi.resetModules();
    const { fetchApi, ApiError } = await import("@/shared/api/client");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 404, code: "MATERIAL_NOT_FOUND", detail: "Không tìm thấy" }), { status: 404 })));
    const error = await fetchApi("/api/v1/materials/x").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as InstanceType<typeof ApiError>).problem).toMatchObject({
      detail: "Not found or you do not have access.", serverDetail: "Không tìm thấy", code: "MATERIAL_NOT_FOUND",
    });
    expect((error as Error).message).toBe("Not found or you do not have access.");
  });
});
