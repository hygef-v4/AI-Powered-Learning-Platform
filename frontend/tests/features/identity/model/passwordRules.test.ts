import { describe, expect, it } from "vitest";
import { checkPassword, passwordAcceptable } from "@/features/identity/model/passwordRules";

describe("passwordRules", () => {
  it("cần ít nhất 8 ký tự và có cả chữ và số", () => {
    expect(passwordAcceptable("hocbai2026")).toBe(true);
    expect(passwordAcceptable("Mật khẩu 1")).toBe(true);
    expect(checkPassword("abc1234")).toEqual({ minLength: false, letterAndDigit: true });
    expect(checkPassword("12345678")).toEqual({ minLength: true, letterAndDigit: false });
    expect(checkPassword("abcdefgh")).toEqual({ minLength: true, letterAndDigit: false });
  });
});
