import { describe, expect, it } from "vitest";
import * as studentLabels from "@/shared/studentCode";

describe("student code helpers (BR-U01-100)", () => {
  it("formats student identity without losing the name or adding an empty separator", () => {
    expect(studentLabels).toHaveProperty("formatStudentLabel");
    expect(studentLabels.formatStudentLabel({ name: "An", studentCode: "SE000001" })).toBe("An · SE000001");
    expect(studentLabels.formatStudentLabel({ name: "An", studentCode: "SE000002" })).toBe("An · SE000002");
    expect(studentLabels.formatStudentLabel({ name: "An", studentCode: null })).toBe("An");
    expect(studentLabels.formatStudentLabel({ name: null, email: "an@school.test" })).toBe("an@school.test");
    expect(studentLabels.formatStudentLabel(undefined)).toBe("Không rõ tên");
  });
});
