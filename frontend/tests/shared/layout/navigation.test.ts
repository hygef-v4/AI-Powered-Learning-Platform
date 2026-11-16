import { describe, expect, it } from "vitest";
import { findActiveHref, extractInitials, type NavItem } from "@/shared/layout/navigation";

const admin: NavItem[] = [
  { href: "/admin", label: "Tổng quan", icon: "monitoring" },
  { href: "/admin/settings", label: "Cài đặt", icon: "tune" },
  { href: "/admin/audit", label: "Nhật ký audit", icon: "history" },
];

describe("findActiveHref", () => {
  it("chọn mục có đường dẫn dài nhất khớp đầu pathname", () => {
    expect(findActiveHref(admin, "/admin")).toBe("/admin");
    expect(findActiveHref(admin, "/admin/settings/files.material.maxSizeMb")).toBe("/admin/settings");
    expect(findActiveHref(admin, "/admin/settingsx")).toBe("/admin");
    expect(findActiveHref(admin, "/profile")).toBeNull();
  });
});

describe("extractInitials", () => {
  it("lấy chữ đầu của hai từ cuối, một từ thì hai chữ đầu", () => {
    expect(extractInitials("Võ Minh Tài")).toBe("MT");
    expect(extractInitials("Trần Thị Hoa")).toBe("TH");
    expect(extractInitials("KienNT")).toBe("KI");
    expect(extractInitials("  ")).toBe("?");
  });
});
