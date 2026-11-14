import { describe, expect, it } from "vitest";
import { isValidStudentCode, matchesPersonQuery, normalizeStudentCode } from "@/shared/studentCode";

describe("student code helpers (BR-U01-100)", () => {
  it("normalizes and validates two letters followed by six digits", () => {
    expect(normalizeStudentCode("  se123456 ")).toBe("SE123456");
    expect(isValidStudentCode("SE123456")).toBe(true);
    expect(isValidStudentCode("se123456")).toBe(false);
    expect(isValidStudentCode("S1234567")).toBe(false);
    expect(isValidStudentCode("SE 12345")).toBe(false);
  });

  it("matches people by name, email or student code without case", () => {
    const person = { name: "Nguyễn Văn A", email: "a@fpt.edu.vn", studentCode: "SE000001" };
    expect(matchesPersonQuery(person, "se0000")).toBe(true);
    expect(matchesPersonQuery(person, "NGUYỄN")).toBe(true);
    expect(matchesPersonQuery(person, "a@fpt")).toBe(true);
    expect(matchesPersonQuery(person, "SE999")).toBe(false);
    expect(matchesPersonQuery({ name: null, email: null }, "")).toBe(true);
  });
});
