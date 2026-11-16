import { describe, expect, it } from "vitest";
import { ACTION_LABELS, describeAction, describeObjectType } from "@/features/audit/model/labels";

describe("nhãn nhật ký", () => {
  it.each(["QUIZ_PUBLISHED", "GROUP_DOC_IMPORTED", "GROUP_PEER_REVIEW_SAVED", "GROUP_ASSIGNMENT_LOCKED", "GROUP_ASSIGNMENT_UNLOCKED", "LOGIN_FAILED", "CLASS_CREATED", "MATERIAL_MODULE_CREATED"])(
    "hành động %s có nhãn tiếng Việt", (code) => {
      expect(describeAction(code)).not.toBe(code);
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
