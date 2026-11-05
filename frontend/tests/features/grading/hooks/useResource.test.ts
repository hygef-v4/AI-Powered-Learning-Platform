import { describe, expect, it } from "vitest";
import { ApiError } from "@/shared/api/client";
import { message } from "@/features/grading/hooks/useResource";

describe("grading conflict explanations", () => {
  it.each([
    ["GRADE_LOCKED", "Điểm đã chốt"],
    ["SUBMISSION_CHANGED", "Bài nộp đã thay đổi"],
    ["NOT_LATEST_ATTEMPT", "Bài nộp đã thay đổi"],
    ["RUBRIC_CHANGED", "Rubric đã thay đổi"],
    ["GRADE_VERSION_CONFLICT", "Dữ liệu đã thay đổi"],
    ["UNKNOWN_CONFLICT", "Dữ liệu đã thay đổi"],
  ])("explains %s", (code, expected) => {
    expect(message(new ApiError({ status: 409, title: "Conflict", code }))).toContain(expected);
  });
  it("keeps existing non-conflict and unknown-error fallbacks", () => {
    expect(message(new Error("Offline"))).toBe("Offline");
    expect(message(null)).toBe("Không thể tải dữ liệu. Vui lòng thử lại.");
  });
});
