import { describe, expect, it } from "vitest";
import type { Role } from "@/shared/auth/roles";
import { accountMenuEntries, navEntries } from "./identity/slots";
import "./register";

function menu(role: Role): string[] {
  return navEntries
    .entries()
    .filter((entry) => entry.roles.includes(role))
    .map((entry) => entry.title);
}

describe("menu bên do các unit đăng ký", () => {
  it("đúng mục và thứ tự của design cho từng role", () => {
    expect(menu("ADMIN")).toEqual(["Tổng quan", "Cài đặt", "Tài khoản", "Môn học", "Agent MCP", "Nhật ký audit"]);
    expect(menu("SUBJECT_MANAGER")).toEqual([
      "Tổng quan",
      "Môn học của tôi",
      "Ngân hàng câu hỏi",
      "Quiz",
      "Học liệu",
      "Bài tập",
      "Lớp học",
      "Lớp được phân công",
      "Thông báo lớp",
    ]);
    expect(menu("TEACHER")).toEqual(["Lớp được phân công", "Thông báo lớp"]);
    expect(menu("STUDENT")).toEqual(["Lớp của tôi", "Bài tập", "Luyện tập Quiz", "Thông báo lớp"]);
  });

  it("Admin không có nút Kết nối AI", () => {
    const forAdmin = accountMenuEntries.entries().filter((entry) => entry.roles.includes("ADMIN"));
    expect(forAdmin).toEqual([]);
    expect(accountMenuEntries.entries().map((entry) => entry.title)).toEqual(["Kết nối AI"]);
  });
});
