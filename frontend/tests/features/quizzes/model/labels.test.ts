import { describe, expect, it } from "vitest";
import { describeViolation } from "@/features/quizzes/model/labels";

// Test chạy với tiếng Việt (tests/locale.setup.ts): câu dịch lấy từ vi.json, không dùng câu của máy chủ (KAN-223).
describe("describeViolation", () => {
  it("dịch lỗi phát hành theo mã, kể cả lỗi theo vị trí câu", () => {
    expect(describeViolation({ field: "items", code: "ITEMS_REQUIRED", message: "server" })).toBe("Quiz cần ít nhất 1 câu.");
    expect(describeViolation({ field: "items[3]", code: "QUESTION_MISSING", message: "server" })).toBe("Câu 3 không còn tồn tại.");
    expect(describeViolation({ field: "activityNotice", code: "ACTIVITY_NOTICE_REQUIRED", message: "server" }))
      .toBe("Cần lời báo cho sinh viên khi bật ghi nhận hoạt động.");
  });

  it("mã chưa có nhãn thì giữ thông điệp của máy chủ", () => {
    expect(describeViolation({ field: "items[2]", code: "OPTION_COUNT", message: "Câu 2: thiếu lựa chọn" })).toBe("Câu 2: thiếu lựa chọn");
  });
});
