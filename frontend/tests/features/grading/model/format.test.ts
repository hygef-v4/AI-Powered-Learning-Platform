import { describe, expect, it } from "vitest";
import { ApiError } from "@/shared/api/client";
import { formatErrorMessage } from "@/features/grading/model/format";

describe("grading conflict explanations", () => {
  it("explains a missing adjustment reason in Vietnamese instead of exposing the server message", () => {
    const error = new ApiError({ status: 400, title: "Bad Request", code: "ADJUSTMENT_REASON_REQUIRED",
      detail: "Adjustment requires a reason" });
    expect(formatErrorMessage(error)).toBe("Nhập lý do khi cộng hoặc trừ điểm.");
  });
  it.each([
    ["GRADE_LOCKED", "Điểm đã chốt"],
    ["SUBMISSION_CHANGED", "Bài nộp đã thay đổi"],
    ["NOT_LATEST_ATTEMPT", "Bài nộp đã thay đổi"],
    ["RUBRIC_CHANGED", "Rubric đã thay đổi"],
    ["GRADE_VERSION_CONFLICT", "Dữ liệu đã thay đổi"],
    ["UNKNOWN_CONFLICT", "Dữ liệu đã thay đổi"],
  ])("explains %s", (code, expected) => {
    expect(formatErrorMessage(new ApiError({ status: 409, title: "Conflict", code }))).toContain(expected);
  });
  it("keeps existing non-conflict and unknown-error fallbacks", () => {
    expect(formatErrorMessage(new Error("Offline"))).toBe("Offline");
    expect(formatErrorMessage(null)).toBe("Không thể tải dữ liệu. Vui lòng thử lại.");
  });
});
