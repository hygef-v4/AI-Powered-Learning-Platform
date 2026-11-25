import { describe, expect, it } from "vitest";
import { buildTranslator } from "@/i18n/msg";
import { describeNotification } from "@/features/notifications/model/labels";

// Test chạy với tiếng Việt (tests/locale.setup.ts): câu tiếng Việt trùng câu U14 lưu, tiếng Anh lấy từ en.json.
describe("describeNotification", () => {
  it("dịch tiêu đề và tóm tắt theo loại thông báo", () => {
    expect(describeNotification({ type: "QUIZ_PUBLISHED", title: "server", summary: "server" }))
      .toEqual({ title: "Có Quiz mới", summary: "Mở Quiz để xem và bắt đầu luyện tập." });
    expect(describeNotification({ type: "ASSIGNMENT_OPENED", title: "server", summary: "server" }).title).toBe("Có bài tập mới mở");
    expect(buildTranslator("en")("notifications.model.labels.quizPublishedTitle")).toBe("A new quiz is available");
    expect(buildTranslator("en")("notifications.model.labels.assignmentOpenedTitle")).toBe("A new assignment is open");
  });

  it("loại chưa biết giữ câu máy chủ gửi", () => {
    expect(describeNotification({ type: "SOMETHING_NEW", title: "Tiêu đề", summary: "Tóm tắt" }))
      .toEqual({ title: "Tiêu đề", summary: "Tóm tắt" });
  });
});
