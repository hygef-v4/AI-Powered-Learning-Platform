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
