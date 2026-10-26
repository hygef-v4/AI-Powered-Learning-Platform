"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Fragment, useCallback, useEffect, useState, type ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { useCurrentUser, type CurrentUser } from "@/shared/auth/useCurrentUser";
import { useTheme } from "@/shared/theme";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";
import { Menu } from "@/shared/ui/Menu";
import { SOFT_TONES } from "@/shared/ui/tones";
import { CommandPalette } from "./CommandPalette";
import { LogoutDialog } from "./LogoutDialog";
import { AVATAR_TONES, ROLE_LABELS, activeHref, initials, type NavItem } from "./navigation";

export type HeaderLink = { href: string; label: string; icon?: IconName };

type Props = {
  role: Role;
  /** Mục sidebar của role, do unit đăng ký qua NavEntry, đã sắp thứ tự. */
  nav: NavItem[];
  /** Lối vào do unit khác đăng ký qua AccountMenuEntry, ví dụ Kết nối AI; nằm trong menu tài khoản. */
  headerLinks?: HeaderLink[];
  /** Thành phần gắn qua slot HeaderWidget, ví dụ chuông thông báo. */
  headerWidgets?: ReactNode;
  /**
   * Người đang đăng nhập do U01 truyền từ phiên (luôn mới sau khi sửa hồ sơ hay đổi tài khoản). Không truyền thì
   * AppShell tự tải `/api/v1/me/profile`.
   */
  user?: CurrentUser | null;
  children: ReactNode;
};

/** Khung sau đăng nhập: sidebar chỉ chứa mục chuyển trang; thanh trên có tìm nhanh, thông báo, tài khoản. */
export function AppShell({ role, nav, headerLinks = [], headerWidgets, user: sessionUser, children }: Props) {
  const pathname = usePathname() ?? "";
  const fetchedUser = useCurrentUser(sessionUser === undefined);
  const user = sessionUser === undefined ? fetchedUser : sessionUser;
  const [theme, toggleTheme] = useTheme();
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [drawer, setDrawer] = useState<string | null>(null);
  const closeLogout = useCallback(() => setLogoutOpen(false), []);
  const closePalette = useCallback(() => setPaletteOpen(false), []);
  const current = activeHref(nav, pathname);
  const roleLabel = ROLE_LABELS[role];
  // Ngăn kéo menu trên màn hẹp tự đóng khi đã sang trang khác.
  const drawerOpen = drawer === pathname;

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setPaletteOpen((open) => !open);
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  const paletteItems = [
    ...nav.map((item) => ({ href: item.href, label: item.label, icon: item.icon, group: item.section })),
    ...headerLinks.map((link) => ({ href: link.href, label: link.label, icon: link.icon, group: "Tài khoản" })),
    { href: "/profile", label: "Hồ sơ", icon: "person" as const, group: "Tài khoản" },
  ];

  return (
    <div data-app-shell className="flex min-h-screen bg-shell">
      <aside
        data-app-sidebar
        className={cx(
          "fixed inset-y-0 left-0 z-40 flex w-[240px] shrink-0 flex-col gap-6 border-r border-line bg-shell px-3 py-5 transition-transform duration-300 ease-[cubic-bezier(.16,1,.3,1)]",
          "md:sticky md:top-0 md:h-screen md:translate-x-0",
          drawerOpen ? "translate-x-0 shadow-[var(--shadow-float)]" : "-translate-x-full",
        )}
      >
        <Link href="/" className="px-2" aria-label="Nền tảng học tập, trang chủ">
          <Logo />
        </Link>
        <nav aria-label={`Menu ${roleLabel}`} className="flex flex-1 flex-col gap-0.5 overflow-y-auto">
          {nav.map((item, index) => {
            const active = item.href === current;
            const heading = item.section && item.section !== nav[index - 1]?.section ? item.section : null;
            return (
              <Fragment key={item.href}>
                {heading && (
                  <span className="mx-2.5 mt-5 mb-1.5 text-[10.5px] font-semibold tracking-[.09em] text-muted uppercase">{heading}</span>
                )}
                <Link
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cx(
                    "group relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13.5px] font-semibold transition-colors duration-200",
                    active ? "bg-primary-soft text-ink" : "text-body hover:bg-sunken hover:text-ink",
                  )}
                >
                  {active && <span aria-hidden className="absolute top-2 bottom-2 -left-3 w-[3px] animate-fade rounded-r bg-primary" />}
                  <Icon
                    name={item.icon}
                    size={17}
                    filled={active}
                    className={cx("transition-colors", active ? "text-primary-ink" : "text-muted group-hover:text-ink")}
                  />
                  <span>{item.label}</span>
                </Link>
              </Fragment>
            );
          })}
        </nav>
      </aside>
      {drawerOpen && (
        <div aria-hidden className="fixed inset-0 z-30 animate-fade bg-[var(--c-scrim)] md:hidden" onClick={() => setDrawer(null)} />
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center gap-2 border-b border-line bg-[var(--c-topbar)] px-4 backdrop-blur-md md:px-8">
          <button
            type="button"
            aria-label="Mở menu"
            onClick={() => setDrawer(pathname)}
            className="inline-flex size-9 items-center justify-center rounded-lg text-body hover:bg-sunken md:hidden"
          >
            <Icon name="menu" size={18} />
          </button>
          <button
            type="button"
            onClick={() => setPaletteOpen(true)}
            className="flex h-9 w-full max-w-[280px] items-center gap-2.5 rounded-lg border border-line bg-surface px-3 text-[13px] text-muted transition-colors hover:border-line-strong"
          >
            <Icon name="search" size={15} />
            <span className="flex-1 text-left">Tìm nhanh</span>
            <kbd className="hidden rounded border border-line-strong bg-sunken px-1.5 font-mono text-[10.5px] sm:inline">Ctrl K</kbd>
          </button>
          <div className="ml-auto flex items-center gap-1">
            {headerWidgets}
            <button
              type="button"
              onClick={toggleTheme}
              aria-label={theme === "dark" ? "Chuyển giao diện sáng" : "Chuyển giao diện tối"}
              title={theme === "dark" ? "Giao diện sáng" : "Giao diện tối"}
              className="inline-flex size-9 items-center justify-center rounded-lg text-body transition-colors hover:bg-sunken hover:text-ink"
            >
              <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
            </button>
            <Menu
              align="right"
              variant="tool"
              ariaLabel="Tài khoản"
              summaryClassName="!h-10 gap-2.5 !pl-1.5 !pr-2"
              label={
                <>
                  <span className={cx("flex size-8 items-center justify-center rounded-full text-[11.5px] font-bold", SOFT_TONES[AVATAR_TONES[role]])}>
                    {user ? initials(user.displayName) : <Icon name="person" size={16} />}
                  </span>
                  <span className="hidden flex-col text-left leading-tight sm:flex">
                    {user && <span className="text-[13px] font-semibold text-ink">{user.displayName}</span>}
                    <span className={user ? "text-[11px] text-muted" : "text-[13px] font-semibold text-ink"}>{roleLabel}</span>
                  </span>
                </>
              }
            >
              <AccountLink href="/profile" icon="person" label="Hồ sơ" />
              {headerLinks.map((link) => (
                <AccountLink key={link.href} href={link.href} icon={link.icon} label={link.label} />
              ))}
              <span className="my-1 h-px bg-line" />
              <button
                type="button"
                data-menu-item
                onClick={() => setLogoutOpen(true)}
                className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-[13px] text-danger transition-colors hover:bg-danger-soft"
              >
                <Icon name="logout" size={16} />
                Đăng xuất
              </button>
            </Menu>
          </div>
        </header>
        <main className="mx-auto flex w-full max-w-[1160px] min-w-0 flex-1 flex-col gap-7 px-4 pt-8 pb-16 sm:px-8 md:pt-10">{children}</main>
      </div>
      <CommandPalette open={paletteOpen} items={paletteItems} onClose={closePalette} />
      <LogoutDialog open={logoutOpen} onClose={closeLogout} />
    </div>
  );
}

function AccountLink({ href, icon, label }: { href: string; icon?: IconName; label: string }) {
  return (
    <Link
      href={href}
      data-menu-item
      className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] text-ink transition-colors hover:bg-sunken"
    >
      {icon && <Icon name={icon} size={16} className="text-muted" />}
      {label}
    </Link>
  );
}
