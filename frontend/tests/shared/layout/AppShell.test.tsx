import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppShell } from "@/shared/layout/AppShell";
import type { NavItem } from "@/shared/layout/navigation";

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
    expect(screen.getByRole("menuitem", { name: "Kết nối AI" })).toHaveAttribute("href", "/ai/connection");
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

  it("logo nằm trên thanh trên, không còn ô Tìm nhanh; nút Menu thu gọn sidebar và nhớ lần sau", () => {
    vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({ matches: true }));
    localStorage.removeItem("sidebar-collapsed");
    const { container, unmount } = render(
      <AppShell role="ADMIN" nav={adminNav} user={null}>
        <p>Nội dung</p>
      </AppShell>,
    );
    expect(container.querySelector("header")).toContainElement(screen.getByRole("link", { name: /trang chủ/ }));
    expect(screen.queryByText("Tìm nhanh")).not.toBeInTheDocument();

    const sidebar = container.querySelector("[data-app-sidebar]")!;
    expect(sidebar).not.toHaveAttribute("data-collapsed");
    fireEvent.click(screen.getByRole("button", { name: "Thu gọn menu" }));
    expect(sidebar).toHaveAttribute("data-collapsed", "true");
    expect(screen.getByRole("button", { name: "Mở menu" })).toHaveAttribute("aria-expanded", "false");
    // Thu gọn: tên mục hiện thành nhãn nổi khi rê chuột, không dùng `title`.
    const settings = screen.getByRole("link", { name: "Cài đặt" });
    expect(settings).not.toHaveAttribute("title");
    fireEvent.mouseEnter(settings);
    expect(container.querySelector("[data-nav-tip]")).toHaveTextContent("Cài đặt");
    fireEvent.mouseLeave(settings);
    expect(container.querySelector("[data-nav-tip]")).not.toBeInTheDocument();
    expect(localStorage.getItem("sidebar-collapsed")).toBe("1");

    unmount();
    const again = render(
      <AppShell role="ADMIN" nav={adminNav} user={null}>
        <p>Nội dung</p>
      </AppShell>,
    );
    expect(again.container.querySelector("[data-app-sidebar]")).toHaveAttribute("data-collapsed", "true");
    localStorage.removeItem("sidebar-collapsed");
  });

  it("bấm Đăng xuất mở popup xác nhận, Hủy thì đóng", () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 401 })));
    render(
      <AppShell role="STUDENT" nav={[]}>
        <p>Nội dung</p>
      </AppShell>,
    );

    // Đăng xuất nằm trong menu tài khoản; sidebar chỉ còn mục chuyển trang.
    const logout = screen.getByRole("menuitem", { name: "Đăng xuất" });
    expect(document.querySelector("[data-app-sidebar]")).not.toContainElement(logout);
    expect(screen.getByRole("menu")).toContainElement(logout);
    fireEvent.click(logout);
    expect(screen.getByRole("dialog", { name: "Đăng xuất?" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("có link bỏ qua tới nội dung trỏ vào vùng main", () => {
    const { container } = render(
      <AppShell role="ADMIN" nav={adminNav} user={null}>
        <p>Nội dung</p>
      </AppShell>,
    );
    expect(container.querySelector("a")).toHaveTextContent("Bỏ qua tới nội dung");
    expect(screen.getByRole("link", { name: "Bỏ qua tới nội dung" })).toHaveAttribute("href", "#main");
    expect(screen.getByRole("main")).toHaveAttribute("id", "main");
  });

  it("màn hẹp: ngăn kéo đóng thì không nhận focus; mở ra thì Esc đóng và trả focus về nút menu", () => {
    vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({ matches: false }));
    const { container } = render(
      <AppShell role="ADMIN" nav={adminNav} user={null}>
        <p>Nội dung</p>
      </AppShell>,
    );
    const sidebar = container.querySelector("[data-app-sidebar]")!;
    expect(sidebar).toHaveAttribute("inert");

    const menuButton = screen.getByRole("button", { name: "Mở menu" });
    fireEvent.click(menuButton);
    expect(sidebar).not.toHaveAttribute("inert");

    // Tab ở mục cuối quay về nút menu, không lọt ra nội dung phía sau.
    const links = sidebar.querySelectorAll("a");
    links[links.length - 1]!.focus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(menuButton).toHaveFocus();

    fireEvent.keyDown(document, { key: "Escape" });
    expect(sidebar).toHaveAttribute("inert");
    expect(menuButton).toHaveFocus();
  });
});
