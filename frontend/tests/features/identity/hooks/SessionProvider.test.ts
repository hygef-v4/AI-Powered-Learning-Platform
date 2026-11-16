import { describe, expect, it } from "vitest";
import { loginPathFor, resolveSafeNextPath } from "@/features/identity/hooks/SessionProvider";

describe("đường dẫn quay lại sau đăng nhập", () => {
  it("giữ trang đang mở trong ?next=, trừ trang gốc và trang công khai", () => {
    expect(loginPathFor({ pathname: "/admin/accounts", search: "?page=2" })).toBe(
      "/login?next=%2Fadmin%2Faccounts%3Fpage%3D2",
    );
    expect(loginPathFor({ pathname: "/", search: "" })).toBe("/login");
    expect(loginPathFor({ pathname: "/activate", search: "" })).toBe("/login");
  });

  it("chỉ nhận đường dẫn nội bộ không phải trang công khai", () => {
    expect(resolveSafeNextPath("/admin/accounts?page=2")).toBe("/admin/accounts?page=2");
    expect(resolveSafeNextPath(null)).toBeNull();
    expect(resolveSafeNextPath("https://evil.example")).toBeNull();
    expect(resolveSafeNextPath("//evil.example")).toBeNull();
    expect(resolveSafeNextPath("/\\evil.example")).toBeNull();
    // Trình duyệt bỏ tab, xuống dòng trong URL: "/\t/evil" thành "//evil".
    expect(resolveSafeNextPath("/\t/evil.example")).toBeNull();
    expect(resolveSafeNextPath("/\n/evil.example")).toBeNull();
    expect(resolveSafeNextPath("/%09/evil.example")).toBe("/%09/evil.example");
    expect(resolveSafeNextPath("/login?next=/x")).toBeNull();
  });
});
