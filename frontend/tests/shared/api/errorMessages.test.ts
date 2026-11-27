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
    expect(describeApiProblem({ status: 400, code: "SETTING_DEPENDENCY", detail: "Cần đặt Giờ chạy hằng ngày trước khi bật mục này" }))
      .toBe("Set the daily run time before turning backup on, and turn backup off before clearing the time.");
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

describe("describeAiFailure (KAN-232)", () => {
  it("translates known AI failure reasons in the English UI", async () => {
    const { describeAiFailure, describeApiProblem } = await loadFor("en");
    expect(describeAiFailure("Bạn đang có 3 yêu cầu AI chưa xong; chờ hoặc hủy bớt rồi thử lại", "No feedback"))
      .toBe("You already have 3 AI requests in progress. Wait for one to finish or cancel one, then try again.");
    expect(describeAiFailure("Người dùng đã hủy", "No feedback")).toBe("You cancelled this request.");
    // Lỗi API có mã AI_ hiện kèm mã, theo quy ước của describeAiCode.
    expect(describeApiProblem({ status: 429, code: "AI_TOO_MANY_TASKS", detail: "Bạn đang có 3 yêu cầu AI chưa xong" }))
      .toBe("[AI_TOO_MANY_TASKS] You already have 3 AI requests in progress. Wait for one to finish or cancel one, then try again.");
    expect(describeAiFailure("[AI_OUTPUT_INVALID] Kết quả AI không đúng định dạng", "No feedback"))
      .toMatch(/^\[AI_OUTPUT_INVALID\] [\x00-\x7F]+$/);
  });

  it("never shows unknown Vietnamese text in the English UI, but keeps English server text", async () => {
    const { describeAiFailure } = await loadFor("en");
    expect(describeAiFailure("Lỗi nhà cung cấp chưa biết", "No feedback received")).toBe("No feedback received");
    expect(describeAiFailure("AI total does not match rubric item scores", "x")).toBe("AI total does not match rubric item scores");
    expect(describeAiFailure(null, "No feedback received")).toBe("No feedback received");
  });

  it("keeps the server text in the Vietnamese UI", async () => {
    const { describeAiFailure } = await loadFor("vi");
    expect(describeAiFailure("Người dùng đã hủy", "Không có góp ý")).toBe("Người dùng đã hủy");
  });
});
