import { describe, expect, it } from "vitest";
import { ACTION_LABELS, describeAction, describeObjectType } from "@/features/audit/model/labels";

describe("nhãn nhật ký", () => {
  it.each(["QUIZ_PUBLISHED", "GROUP_DOC_IMPORTED", "GROUP_ASSIGNMENT_LOCKED", "GROUP_ASSIGNMENT_UNLOCKED", "LOGIN_FAILED", "CLASS_CREATED", "MATERIAL_MODULE_CREATED"])(
    "hành động %s có nhãn tiếng Việt", (code) => {
      expect(describeAction(code)).not.toBe(code);
    });

  it.each([
    ["SUBJECT_CS101_CREATED", "Tạo môn CS101"],
    ["SUBJECT_SE-101_ARCHIVED", "Lưu trữ môn SE-101"],
    ["SUBJECT_CS101_REOPENED", "Mở lại môn CS101"],
    ["CLASS_SE1801_CREATED", "Tạo lớp SE1801"],
    ["CLASS_SE1801_OPENED", "Mở lớp SE1801"],
    ["CLASS_SE_1801_ARCHIVED", "Lưu trữ lớp SE_1801"],
    ["CLASS_SE1801_REOPENED", "Mở lại lớp SE1801"],
  ])("môn, lớp ghi kèm mã: %s hiện %s", (code, label) => {
    expect(describeAction(code)).toBe(label);
  });

  it.each(["ROLE_CHANGE_REJECTED", "FILE_UPLOAD", "FILE_DOWNLOAD", "PASSWORD_RESET", "STUDENT_ENROLLED"])(
    "mã cũ %s vẫn có nhãn nhưng không còn trong ô lọc", (code) => {
      expect(describeAction(code)).not.toBe(code);
      expect(ACTION_LABELS).not.toHaveProperty(code);
    });

  it("đối tượng tệp, cấu hình AI và bảng điều khiển có nhãn", () => {
    expect(describeObjectType("FILE")).toBe("Tệp");
    expect(describeObjectType("AI")).toBe("Cấu hình AI");
    expect(describeObjectType("ADMIN_DASHBOARD")).toBe("Bảng điều khiển quản trị");
  });

  it("mã chưa có nhãn vẫn hiện nguyên mã", () => {
    expect(describeAction("NEW_ACTION")).toBe("NEW_ACTION");
  });

  it("đối tượng lớp và học liệu có nhãn", () => {
    expect(describeObjectType("COURSE_CLASS")).toBe("Lớp");
    expect(describeObjectType("MATERIAL_MODULE")).toBe("Module học liệu");
    expect(describeObjectType("QUIZ_ATTEMPT")).toBe("Lượt làm quiz");
  });

  it("không có nhãn trùng nhau, để ô lọc không có hai lựa chọn giống hệt", () => {
    const labels = Object.values(ACTION_LABELS);
    expect(new Set(labels).size).toBe(labels.length);
  });
});
