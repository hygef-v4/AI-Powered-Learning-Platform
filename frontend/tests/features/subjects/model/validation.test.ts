import { describe, expect, it } from "vitest";
import { normalizeSubjectCode, validateCreateSubject, validateSubjectInformation } from "@/features/subjects/model/validation";

describe("validation môn học — BR-U04-02/06, Q09", () => {
  it("chuẩn hóa mã và kiểm độ dài sau trim", () => {
    expect(normalizeSubjectCode("  swr-302 ")).toBe("SWR-302");
    expect(validateCreateSubject({ code: "A".repeat(20), name: " N ", description: null })).toEqual({});
    expect(validateCreateSubject({ code: "1-A", name: "Name" })).toEqual({});
  });

  it.each(["", "   ", "SWR 302", "SWR_302", "SWR/302", "MÔN", "A".repeat(21)])("từ chối mã %j", (code) => {
    expect(validateCreateSubject({ code, name: "Name" }).code).toBeDefined();
  });

  it("kiểm tên trắng, tên200/201 và mô tả2000/2001 như domain Java", () => {
    expect(validateSubjectInformation({ name: " \t\n " }).name).toBeDefined();
    expect(validateSubjectInformation({ name: "N".repeat(200), description: "D".repeat(2000) })).toEqual({});
    expect(validateSubjectInformation({ name: "N".repeat(201), description: "D".repeat(2001) }))
      .toHaveProperty("description");
    expect(validateSubjectInformation({ name: "N".repeat(201) }).name).toBeDefined();
    expect(validateSubjectInformation({ name: "Name", description: null })).toEqual({});
  });

  it("edit thông tin không yêu cầu mã môn hoặc tự thay nội dung mô tả", () => {
    const input = { name: " Name ", description: "  Nội dung  " };
    expect(validateSubjectInformation(input)).toEqual({});
    expect(input.description).toBe("  Nội dung  ");
  });
});
