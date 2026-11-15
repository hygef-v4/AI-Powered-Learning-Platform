import { describe, expect, it } from "vitest";
import { shortTestResult } from "@/features/ai/model/labels";

describe("kết quả kiểm tra kết nối AI", () => {
  it.each([
    ["Đạt", "Đạt"],
    ["AI đã đạt giới hạn token của lần gọi nên phản hồi chưa hoàn chỉnh.", "Giới hạn token"],
    ["Nhà cung cấp AI đang giới hạn tần suất gọi. Vui lòng chờ rồi thử lại.", "Giới hạn tần suất"],
    ["Tài khoản API đã hết hạn mức hoặc số dư.", "Hết hạn mức"],
    ["Nhà cung cấp AI báo vượt giới hạn tần suất hoặc hết hạn mức (HTTP 429)", "Tần suất / hạn mức"],
    ["Lỗi HTTP 429", "Tần suất / hạn mức"],
    ["Key API không hợp lệ hoặc không có quyền (HTTP 401)", "Sai khóa"],
    ["Model không tồn tại (HTTP 404)", "Sai model"],
    ["Nhà cung cấp AI không trả lời kịp", "Quá giờ"],
    ["Mất kết nối với nhà cung cấp AI", "Mất kết nối"],
    [null, "Không đạt"],
  ])("%s → %s", (message, label) => {
    expect(shortTestResult(message)).toBe(label);
  });
});
