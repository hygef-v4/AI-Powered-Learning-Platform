import { describe, expect, it } from "vitest";
import { activeHref, initials, type NavItem } from "./navigation";

const admin: NavItem[] = [
  { href: "/admin", label: "Tổng quan", icon: "monitoring" },
  { href: "/admin/settings", label: "Cài đặt", icon: "tune" },
  { href: "/admin/audit", label: "Nhật ký audit", icon: "history" },
];

describe("activeHref", () => {
  it("chọn mục có đường dẫn dài nhất khớp đầu pathname", () => {
    expect(activeHref(admin, "/admin")).toBe("/admin");
    expect(activeHref(admin, "/admin/settings/files.material.maxSizeMb")).toBe("/admin/settings");
    expect(activeHref(admin, "/admin/settingsx")).toBe("/admin");
    expect(activeHref(admin, "/profile")).toBeNull();
  });
});

describe("initials", () => {
  it("lấy chữ đầu của hai từ cuối, một từ thì hai chữ đầu", () => {
    expect(initials("Võ Minh Tài")).toBe("MT");
    expect(initials("Trần Thị Hoa")).toBe("TH");
    expect(initials("KienNT")).toBe("KI");
    expect(initials("  ")).toBe("?");
  });
});
