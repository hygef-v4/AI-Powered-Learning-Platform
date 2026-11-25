import { describe, expect, it } from "vitest";
import { buildTranslator } from "@/i18n/msg";
import { NUMBER_LOCALE } from "@/i18n/number";
import { describeSourceWarning, describeViolation, formatPoints } from "@/features/quizzes/model/labels";
import { describeReliability } from "@/shared/activity/ActivityTimeline";

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

describe("describeReliability", () => {
  it("ghép số chỗ không rõ vào ghi chú độ tin cậy", () => {
    expect(describeReliability(0)).toBe("Dữ liệu chỉ là dấu hiệu rời trang, không kết luận gian lận.");
    expect(describeReliability(2)).toBe("Có 2 chỗ thiếu hoặc lệch dữ liệu, ghi là không rõ. Dữ liệu chỉ là dấu hiệu rời trang, không kết luận gian lận.");
  });
});

describe("describeSourceWarning", () => {
  it("dịch hai cảnh báo nguồn tài liệu cố định, câu lạ giữ nguyên", () => {
    const retried = "Câu hỏi AI tạo có thể không bám tài liệu đã chọn. Hệ thống bỏ prompt bổ sung và tạo lại một lần chỉ theo tài liệu nguồn.";
    expect(describeSourceWarning(retried)).toBe(retried);
    expect(buildTranslator("en")("quizzes.model.labels.requestNotMatchingSources"))
      .toBe("Your request may not relate to the selected documents; the questions were created from the source documents.");
    expect(describeSourceWarning("Cảnh báo khác")).toBe("Cảnh báo khác");
  });
});

describe("formatPoints", () => {
  it("viết số thập phân theo ngôn ngữ giao diện", () => {
    expect(NUMBER_LOCALE).toBe("vi-VN");
    expect(formatPoints(3.5)).toBe("3,5");
  });
});
