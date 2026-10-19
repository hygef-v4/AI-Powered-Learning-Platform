"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useState, type ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { useCurrentUser, type CurrentUser } from "@/shared/auth/useCurrentUser";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { SOFT_TONES } from "@/shared/ui/tones";
import { LogoutDialog } from "./LogoutDialog";
import { AVATAR_TONES, ROLE_LABELS, activeHref, initials, type NavItem } from "./navigation";

export type HeaderLink = { href: string; label: string; icon?: IconName };

type Props = {
  role: Role;
  /** Mục menu bên của role, do unit đăng ký qua NavEntry, đã sắp thứ tự. */
  nav: NavItem[];
  /** Lối vào do unit khác đăng ký qua AccountMenuEntry, ví dụ Kết nối AI. */
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

/** Khung sau đăng nhập: thanh trên cùng, menu bên theo vai trò, vùng nội dung. */
export function AppShell({ role, nav, headerLinks = [], headerWidgets, user: sessionUser, children }: Props) {
  const pathname = usePathname() ?? "";
  const fetchedUser = useCurrentUser(sessionUser === undefined);
  const user = sessionUser === undefined ? fetchedUser : sessionUser;
  const [logoutOpen, setLogoutOpen] = useState(false);
  const closeLogout = useCallback(() => setLogoutOpen(false), []);
  const current = activeHref(nav, pathname);
  const roleLabel = ROLE_LABELS[role];

  return (
    <div className="flex min-h-screen flex-col bg-shell">
      <header className="sticky top-0 z-30 flex h-16 w-full shrink-0 items-center justify-between border-b border-line bg-white px-6">
        <Link href="/" className="flex items-center gap-3">
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-white shadow-sm">
            <Icon name="school" filled />
          </span>
          <span className="text-[17px] font-bold tracking-tight text-ink">Nền tảng học tập</span>
        </Link>
        <nav aria-label="Thanh điều hướng" className="flex items-center gap-2">
          {headerLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-[13px] font-medium text-body transition-colors hover:bg-shell hover:text-ink"
            >
              {link.icon && <Icon name={link.icon} size={18} />}
              <span>{link.label}</span>
            </Link>
          ))}
          {headerWidgets}
          <Link
            href="/profile"
            title="Hồ sơ"
            className="ml-1 flex items-center gap-2.5 rounded-xl py-1.5 pr-3 pl-2 transition-colors hover:bg-shell"
          >
            <span
              className={cx(
                "flex size-8 items-center justify-center rounded-full text-xs font-bold",
                SOFT_TONES[AVATAR_TONES[role]],
              )}
            >
              {user ? initials(user.displayName) : <Icon name="person" size={18} />}
            </span>
            <span className="flex flex-col text-left leading-tight">
              {user && <span className="text-[13px] font-semibold text-ink">{user.displayName}</span>}
              <span className={user ? "text-[11px] text-muted" : "text-[13px] font-semibold text-ink"}>{roleLabel}</span>
            </span>
          </Link>
          <button
            type="button"
            onClick={() => setLogoutOpen(true)}
            className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-[13px] font-medium text-danger hover:bg-danger-soft"
          >
            <Icon name="logout" size={18} />
            <span>Đăng xuất</span>
          </button>
        </nav>
      </header>
      <div className="flex min-h-[calc(100vh-64px)] flex-1">
        <aside className="w-[236px] shrink-0 border-r border-line bg-white px-3 py-5">
          <nav aria-label={`Menu ${roleLabel}`} className="flex flex-col gap-1">
            {nav.map((item) => {
              const active = item.href === current;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cx(
                    "flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-[13px] transition-colors",
                    active
                      ? "bg-primary font-semibold text-white shadow-sm"
                      : "font-medium text-body hover:bg-shell hover:text-ink",
                  )}
                >
                  <Icon name={item.icon} filled={active} className={active ? undefined : "text-muted"} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </aside>
        <main className="flex min-w-0 flex-1 flex-col gap-5 bg-canvas px-8 pt-7 pb-10">{children}</main>
      </div>
      <LogoutDialog open={logoutOpen} onClose={closeLogout} />
    </div>
  );
}
