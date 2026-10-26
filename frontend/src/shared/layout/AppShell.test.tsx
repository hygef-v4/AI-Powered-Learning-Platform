import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppShell } from "./AppShell";
import type { NavItem } from "./navigation";

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/settings/backup.enabled",
  useRouter: () => ({ replace: vi.fn() }),
}));

const adminNav: NavItem[] = [
  { href: "/admin", label: "Tổng quan", icon: "monitoring" },
  { href: "/admin/settings", label: "Cài đặt", icon: "tune" },
  { href: "/admin/audit", label: "Nhật ký audit", icon: "history" },
];

describe("AppShell", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hiện menu của vai trò, đánh dấu mục đang mở và hồ sơ người dùng", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ displayName: "Phòng Đào tạo" }), { status: 200 })),
    );
    render(
      <AppShell role="ADMIN" nav={adminNav} headerLinks={[{ href: "/ai/connection", label: "Kết nối AI", icon: "smart_toy" }]}>
        <p>Nội dung</p>
      </AppShell>,
    );

    const menu = screen.getByRole("navigation", { name: "Menu Quản trị viên" });
    expect(menu).toHaveTextContent("Nhật ký audit");
    expect(screen.getByRole("link", { name: "Cài đặt" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Tổng quan" })).not.toHaveAttribute("aria-current");
    expect(screen.getByRole("link", { name: "Kết nối AI" })).toHaveAttribute("href", "/ai/connection");
    expect(await screen.findByText("Phòng Đào tạo")).toBeInTheDocument();
    expect(screen.getByText("Quản trị viên")).toBeInTheDocument();
  });

  it("có hồ sơ từ phiên thì dùng luôn, không tải lại", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(
      <AppShell role="STUDENT" nav={[]} user={{ displayName: "Vũ Minh Tài" }}>
        <p>Nội dung</p>
      </AppShell>,
    );

    expect(screen.getByText("Vũ Minh Tài")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("vẽ tiêu đề nhóm một lần phía trên mục đầu tiên của nhóm", () => {
    render(
      <AppShell
        role="SUBJECT_MANAGER"
        nav={[
          { href: "/manager/classes", label: "Lớp học", icon: "groups" },
          { href: "/classes", label: "Lớp được phân công", icon: "class", section: "Giảng dạy" },
        ]}
      >
        <p>Nội dung</p>
      </AppShell>,
    );
    expect(screen.getAllByText("Giảng dạy")).toHaveLength(1);
  });

  it("bấm Đăng xuất mở popup xác nhận, Hủy thì đóng", () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 401 })));
    render(
      <AppShell role="STUDENT" nav={[]}>
        <p>Nội dung</p>
      </AppShell>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));
    expect(screen.getByRole("dialog", { name: "Đăng xuất?" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
