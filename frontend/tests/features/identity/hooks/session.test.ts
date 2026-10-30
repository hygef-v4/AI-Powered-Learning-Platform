import { describe, expect, it } from "vitest";
import { loginPathFor, safeNextPath } from "@/features/identity/hooks/session";

describe("đường dẫn quay lại sau đăng nhập", () => {
  it("giữ trang đang mở trong ?next=, trừ trang gốc và trang công khai", () => {
    expect(loginPathFor({ pathname: "/admin/accounts", search: "?page=2" })).toBe(
      "/login?next=%2Fadmin%2Faccounts%3Fpage%3D2",
    );
    expect(loginPathFor({ pathname: "/", search: "" })).toBe("/login");
    expect(loginPathFor({ pathname: "/activate", search: "" })).toBe("/login");
  });

  it("chỉ nhận đường dẫn nội bộ không phải trang công khai", () => {
    expect(safeNextPath("/admin/accounts?page=2")).toBe("/admin/accounts?page=2");
    expect(safeNextPath(null)).toBeNull();
    expect(safeNextPath("https://evil.example")).toBeNull();
    expect(safeNextPath("//evil.example")).toBeNull();
    expect(safeNextPath("/\\evil.example")).toBeNull();
    expect(safeNextPath("/login?next=/x")).toBeNull();
  });
});
