import { describe, expect, it } from "vitest";
import { checkPassword, isPasswordAcceptable } from "@/features/identity/model/passwordRules";

describe("passwordRules", () => {
  it("cần ít nhất 8 ký tự và có cả chữ và số", () => {
    expect(isPasswordAcceptable("hocbai2026")).toBe(true);
    expect(isPasswordAcceptable("Mật khẩu 1")).toBe(true);
    expect(checkPassword("abc1234")).toEqual({ minLength: false, letterAndDigit: true });
    expect(checkPassword("12345678")).toEqual({ minLength: true, letterAndDigit: false });
    expect(checkPassword("abcdefgh")).toEqual({ minLength: true, letterAndDigit: false });
  });
});
