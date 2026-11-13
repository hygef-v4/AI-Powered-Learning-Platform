import { describe, expect, it } from "vitest";
import type { Role } from "@/shared/auth/roles";
import { accountMenuEntries, navEntries, subjectNavEntries } from "@/features/identity/slots";
import "@/features/register";

function menu(role: Role): string[] {
  return navEntries
    .entries()
    .filter((entry) => entry.roles.includes(role))
    .map((entry) => entry.title);
}

describe("menu bên do các unit đăng ký", () => {
  it("đúng mục và thứ tự của design cho từng role", () => {
    expect(menu("ADMIN")).toEqual(["Tổng quan", "Tài khoản", "Môn học", "Cài đặt", "Nhật ký"]);
    expect(menu("SUBJECT_MANAGER")).toEqual(["Môn được phân công", "Lớp được phân công", "Tất cả thông báo"]);
    expect(subjectNavEntries.entries().map((entry) => entry.title)).toEqual([
      "Thông tin môn",
      "Lớp học",
      "Học liệu",
      "Đề mẫu",
      "Ngân hàng câu hỏi",
    ]);
    expect(menu("TEACHER")).toEqual(["Lớp được phân công", "Tất cả thông báo"]);
    expect(menu("STUDENT")).toEqual(["Lớp của tôi", "Bài tập", "Tất cả thông báo"]);
  });

  it("Admin không có nút Kết nối AI", () => {
    const forAdmin = accountMenuEntries.entries().filter((entry) => entry.roles.includes("ADMIN"));
    expect(forAdmin).toEqual([]);
    expect(accountMenuEntries.entries().map((entry) => entry.title)).toEqual(["Kết nối AI"]);
  });
});
