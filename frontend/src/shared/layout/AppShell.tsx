"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import {
  Fragment,
  Suspense,
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
  type FocusEvent,
  type MouseEvent,
  type ReactNode,
} from "react";
import type { Role } from "@/shared/auth/roles";
import { useCurrentUser, type CurrentUser } from "@/shared/auth/useCurrentUser";
import { useTheme } from "@/shared/theme";
import { cx } from "@/shared/ui/cx";
import { Icon, type IconName } from "@/shared/ui/Icon";
import { Logo } from "@/shared/ui/Logo";
import { Menu, MenuItem } from "@/shared/ui/Menu";
import { SOFT_TONES } from "@/shared/ui/tones";
import { LogoutDialog } from "./LogoutDialog";
import { AVATAR_TONES, FADE_LABEL, ROLE_LABELS, activeHref, initials, type NavContext, type NavItem, type NavLocation } from "./navigation";

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
  /** Sidebar theo ngữ cảnh (môn, lớp) cho trang đang mở; trả `null` thì dùng `nav` của role. */
  context?: (at: NavLocation) => NavContext | null;
  children: ReactNode;
};

const COLLAPSED_KEY = "sidebar-collapsed";

const WIDE = "(min-width: 768px)";

/** Màn rộng (từ `md`): sidebar thu gọn được; màn hẹp: sidebar là ngăn kéo. */
function useWide() {
  return useSyncExternalStore(
    (onChange) => {
      const query = typeof window.matchMedia === "function" ? window.matchMedia(WIDE) : null;
      query?.addEventListener?.("change", onChange);
      return () => query?.removeEventListener?.("change", onChange);
    },
    () => (typeof window.matchMedia === "function" ? window.matchMedia(WIDE).matches : true),
    () => true,
  );
}

function storedCollapsed() {
  try {
    return typeof window !== "undefined" && localStorage.getItem(COLLAPSED_KEY) === "1";
  } catch {
    return false;
  }
}

const FOCUSABLE = 'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), [tabindex="0"]';

/**
 * Khung sau đăng nhập: thanh trên dài hết chiều ngang (nút menu, logo, thông báo, theme, tài khoản kèm đăng xuất); bên
 * dưới là sidebar chỉ chứa mục chuyển trang và nội dung. Ba vùng mỗi vùng một màu. Màn rộng thu gọn được sidebar còn biểu
 * tượng (nhớ qua lần sau); màn hẹp sidebar là ngăn kéo: đóng thì không nhận focus, mở thì giữ Tab trong ngăn kéo và nút
 * menu, Esc để đóng.
 */
export function AppShell({ role, nav, headerLinks = [], headerWidgets, user: sessionUser, context, children }: Props) {
  const pathname = usePathname() ?? "";
  const fetchedUser = useCurrentUser(sessionUser === undefined);
  const user = sessionUser === undefined ? fetchedUser : sessionUser;
  const [theme, toggleTheme] = useTheme();
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [drawer, setDrawer] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState(storedCollapsed);
  const wide = useWide();
  const closeLogout = useCallback(() => setLogoutOpen(false), []);
  const menuButton = useRef<HTMLButtonElement>(null);
  const sidebar = useRef<HTMLElement>(null);
  const roleLabel = ROLE_LABELS[role];
  // Ngăn kéo menu trên màn hẹp tự đóng khi đã sang trang khác.
  const drawerOpen = drawer === pathname;

  // Sidebar đang hiện đủ: màn rộng là chưa thu gọn, màn hẹp là ngăn kéo đang mở.
  const sidebarOpen = wide ? !collapsed : drawerOpen;

  const drawerActive = !wide && drawerOpen;
  useEffect(() => {
    if (!drawerActive) return;
    function handleDrawerKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setDrawer(null);
        menuButton.current?.focus();
        return;
      }
      if (event.key !== "Tab" || !menuButton.current || !sidebar.current) return;
      const stops = [menuButton.current, ...sidebar.current.querySelectorAll<HTMLElement>(FOCUSABLE)];
      const first = stops[0]!;
      const last = stops.at(-1)!;
      const current = document.activeElement as HTMLElement | null;
      if (!current || !stops.includes(current)) {
        event.preventDefault();
        first.focus();
      } else if (event.shiftKey && current === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && current === last) {
        event.preventDefault();
        first.focus();
      }
    }
    document.addEventListener("keydown", handleDrawerKey);
    return () => document.removeEventListener("keydown", handleDrawerKey);
  }, [drawerActive]);

  function toggleSidebar() {
    if (!wide) {
      setDrawer(drawerOpen ? null : pathname);
      return;
    }
    const next = !collapsed;
    setCollapsed(next);
    try {
      localStorage.setItem(COLLAPSED_KEY, next ? "1" : "0");
    } catch {
      // Trình duyệt chặn lưu trữ: chỉ nhớ trong phiên này.
    }
  }

  return (
    <div data-app-shell className="flex min-h-screen flex-col bg-shell">
      <a
        href="#main"
        className="sr-only focus-visible:not-sr-only focus-visible:fixed focus-visible:top-2 focus-visible:left-2 focus-visible:z-[120] focus-visible:rounded-lg focus-visible:bg-primary focus-visible:px-3.5 focus-visible:py-2 focus-visible:text-[13px] focus-visible:font-semibold focus-visible:text-on-primary"
      >
        Bỏ qua tới nội dung
      </a>
      <header className="sticky top-0 z-40 flex h-14 shrink-0 items-center gap-1.5 border-b border-line bg-nav pr-3 pl-2.5 md:pr-4">
        <button
          ref={menuButton}
          type="button"
          aria-label={sidebarOpen ? "Thu gọn menu" : "Mở menu"}
          aria-expanded={sidebarOpen}
          title={sidebarOpen ? "Thu gọn menu" : "Mở menu"}
          onClick={toggleSidebar}
          className="inline-flex size-10 shrink-0 items-center justify-center rounded-lg text-body transition-colors hover:bg-sunken hover:text-ink"
        >
          <ToggleIcon open={sidebarOpen} />
        </button>
        <Link href="/" className="rounded-lg px-1.5 py-1" aria-label="Nền tảng học tập, trang chủ">
          <Logo className="[&>span:last-child]:hidden sm:[&>span:last-child]:inline" />
        </Link>
        <div className="ml-auto flex items-center gap-1">
          <button
            type="button"
            onClick={toggleTheme}
            aria-label={theme === "dark" ? "Chuyển giao diện sáng" : "Chuyển giao diện tối"}
            title={theme === "dark" ? "Giao diện sáng" : "Giao diện tối"}
            className="inline-flex size-9 items-center justify-center rounded-lg text-body transition-colors hover:bg-sunken hover:text-ink"
          >
            <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
          </button>
          {headerWidgets}
          <Menu
            align="right"
            variant="tool"
            ariaLabel="Tài khoản"
            summaryClassName="!h-10 gap-2.5 !pl-1.5 !pr-2"
            label={
              <>
                <span className={cx("flex size-8 items-center justify-center rounded-full text-xs font-bold", SOFT_TONES[AVATAR_TONES[role]])}>
                  {user ? initials(user.displayName) : <Icon name="person" size={16} />}
                </span>
                <span className="hidden flex-col text-left leading-tight sm:flex">
                  {user && <span className="text-[13px] font-semibold text-ink">{user.displayName}</span>}
                  <span className={user ? "text-xs text-muted" : "text-[13px] font-semibold text-ink"}>{roleLabel}</span>
                </span>
              </>
            }
          >
            <AccountLink href="/profile" icon="person" label="Hồ sơ" />
            {headerLinks.map((link) => (
              <AccountLink key={link.href} href={link.href} icon={link.icon} label={link.label} />
            ))}
            <hr className="my-1 border-line" />
            <MenuItem icon="logout" tone="danger" onClick={() => setLogoutOpen(true)}>
              Đăng xuất
            </MenuItem>
          </Menu>
        </div>
      </header>

      <div className="flex min-w-0 flex-1">
        <aside
          ref={sidebar}
          data-app-sidebar
          data-collapsed={collapsed || undefined}
          // Màn hẹp, ngăn kéo đóng: nằm ngoài màn hình nên không được nhận focus hay để trình đọc màn hình đọc.
          inert={!wide && !drawerOpen}
          className={cx(
            "group/sidebar fixed top-14 bottom-0 left-0 z-30 flex w-[256px] shrink-0 flex-col border-r border-line bg-sidebar px-3 py-4",
            "md:sticky md:h-[calc(100dvh-3.5rem)] md:translate-x-0",
            // Thu gọn chỉ hẹp lại, giữ nguyên lề: biểu tượng đứng yên chỗ cũ; còn 44px bên trong cho mã môn, lớp.
            collapsed && "md:w-[68px]",
            drawerOpen ? "translate-x-0 shadow-[var(--shadow-float)]" : "-translate-x-full",
          )}
        >
          {context ? (
            <Suspense fallback={<NavList label={`Menu ${roleLabel}`} items={nav} pathname={pathname} collapsed={collapsed} />}>
              <ContextualNav resolve={context} nav={nav} label={`Menu ${roleLabel}`} pathname={pathname} collapsed={collapsed} />
            </Suspense>
          ) : (
            <NavList label={`Menu ${roleLabel}`} items={nav} pathname={pathname} collapsed={collapsed} />
          )}
        </aside>
        {drawerOpen && (
          <div aria-hidden className="fixed inset-x-0 top-14 bottom-0 z-20 animate-fade bg-[var(--c-scrim)] md:hidden" onClick={() => setDrawer(null)} />
        )}
        <div className="relative isolate flex min-w-0 flex-1 flex-col overflow-x-clip">
          {/* Nội dung rộng theo phần trăm màn hình (lề hai bên 4%), không chặn ở một độ rộng cố định: màn lớn không còn khoảng
              trống to hai bên. */}
          <main id="main" tabIndex={-1} className="main-gutter flex w-full min-w-0 flex-1 flex-col gap-7 pt-8 pb-16 outline-none md:pt-10">
            {children}
          </main>
        </div>
      </div>
      <LogoutDialog open={logoutOpen} onClose={closeLogout} />
    </div>
  );
}

/**
 * Chọn sidebar cho trang đang mở: ngữ cảnh thay menu (đề cương bài, module học liệu) nếu khớp; không thì menu của role, mỗi
 * mục có cây (môn, lớp) luôn kèm nhánh của nó (yêu cầu 2026-11-10), nhánh của trang đang mở thì biết môn, lớp đang mở.
 */
function ContextualNav({ resolve, nav, label, pathname, collapsed }: { resolve: (at: NavLocation) => NavContext | null; nav: NavItem[]; label: string; pathname: string; collapsed: boolean }) {
  const params = useSearchParams();
  const found = resolve({ pathname, search: new URLSearchParams(params?.toString() ?? "") });
  if (found && !found.parentHref) {
    return <NavList key={found.key} label={found.label} items={found.items} pathname={pathname} header={found.header} collapsed={collapsed} />;
  }
  const branches = new Map<string, NavBranch>();
  for (const item of nav) {
    // Nhánh của mục chưa mở trang nào bên trong: dựng như đang ở trang danh sách của mục đó (chưa chọn môn, lớp).
    const here = found?.parentHref === item.href ? found : resolve({ pathname: item.href, search: new URLSearchParams() });
    if (here?.parentHref === item.href) branches.set(item.href, { content: here.header, current: here === found });
  }
  return <NavList key="role" label={label} items={nav} pathname={pathname} collapsed={collapsed} branches={branches} />;
}

/** `current`: trang đang mở nằm trong nhánh này. */
type NavBranch = { content: ReactNode; current: boolean };

/**
 * Danh sách mục sidebar; đổi sang ngữ cảnh khác thì dựng lại để mờ vào. Sidebar thu gọn (chỉ màn rộng) thì còn biểu
 * tượng; tên mục hiện thành nhãn nổi bên phải khi rê chuột hoặc focus bằng bàn phím, trình đọc màn hình vẫn đọc tên mục.
 * `branches`: nhánh cây (môn, lớp) dưới mục cha. Mũi tên ở
 * cuối hàng mục cha chỉ mở, đóng nhánh, không chuyển trang (yêu cầu 2026-11-10), để chọn môn, lớp rồi mục bên trong; bấm tên
 * mục cha thì sang trang danh sách như mọi mục khác và mở nhánh. Không thêm nút nào. Mục cha chỉ tô như mục đang mở khi đang ở
 * đúng trang của nó hoặc khi sidebar thu gọn.
 */
function NavList({ label, items, pathname, header, collapsed, branches }: { label: string; items: NavItem[]; pathname: string; header?: ReactNode; collapsed: boolean; branches?: Map<string, NavBranch> }) {
  const inBranch = [...(branches?.entries() ?? [])].find(([, branch]) => branch.current)?.[0];
  const current = inBranch ?? activeHref(items, pathname);
  const [toggled, setToggled] = useState<Record<string, boolean>>({});
  const isOpen = (href: string) => toggled[href] ?? (href === inBranch || pathname === href);
  const [tip, setTip] = useState<{ label: string; top: number } | null>(null);

  /** Nhãn nổi đặt theo sidebar (nằm ngoài vùng cuộn của danh sách nên không bị cắt). */
  function showTip(event: MouseEvent<HTMLElement> | FocusEvent<HTMLElement>, label: string) {
    if (!collapsed) return;
    if (event.type === "focus" && !event.currentTarget.matches(":focus-visible")) return;
    const row = event.currentTarget.getBoundingClientRect();
    const base = event.currentTarget.closest("[data-app-sidebar]")?.getBoundingClientRect().top ?? 0;
    setTip({ label, top: row.top - base + row.height / 2 });
  }
  // Phần đầu (lối về, ô chọn môn hay lớp) nằm ngoài vùng cuộn để danh sách thả xuống của ô chọn tràn ra được khi sidebar thu gọn.
  return (
    <div className="flex min-h-0 flex-1 animate-fade flex-col">
      {header}
      {/* Ngữ cảnh tự vẽ danh sách trong phần đầu (ví dụ danh sách module) thì không có mục riêng. */}
      {items.length > 0 && <nav aria-label={label} className="flex min-h-0 flex-1 flex-col gap-0.5 overflow-x-hidden overflow-y-auto">
      {items.map((item, index) => {
        const branch = branches?.get(item.href);
        const isParent = !!branch;
        const open = isParent && isOpen(item.href);
        // Mục cha của nhánh khi đang ở trang con: chỉ tô khi sidebar thu gọn (màn rộng), lúc mục con bị ẩn.
        const ancestor = isParent && pathname !== item.href;
        const active = item.active ?? item.href === current;
        const heading = item.section && item.section !== items[index - 1]?.section ? item.section : null;
        return (
          <Fragment key={item.href}>
            {heading && (
              <span className={cx("mx-2.5 mt-5 mb-1.5 text-xs font-semibold tracking-[.09em] whitespace-nowrap text-muted uppercase", FADE_LABEL)}>
                {heading}
              </span>
            )}
            <div className="relative">
            <Link
              href={item.href}
              onMouseEnter={(event) => showTip(event, item.label)}
              onMouseLeave={() => setTip(null)}
              onFocus={(event) => showTip(event, item.label)}
              onBlur={() => setTip(null)}
              aria-current={active && !ancestor ? "page" : undefined}
              onClick={isParent ? () => setToggled((previous) => ({ ...previous, [item.href]: true })) : undefined}
              className={cx(
                "group relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13.5px] font-semibold whitespace-nowrap transition-colors duration-200",
                // Mục đang chọn tô primary-soft (xanh dành cho trạng thái chọn), hover tô sidebar-hover: nền sidebar trắng nên
                // không dùng surface (trắng trên trắng).
                isParent && "pr-10",
                ancestor
                  ? "text-ink hover:bg-sidebar-hover md:group-data-[collapsed]/sidebar:bg-primary-soft"
                  : active
                    ? "bg-primary-soft text-ink"
                    : "text-body hover:bg-sidebar-hover hover:text-ink",
              )}
            >
              {active && (
                <span
                  aria-hidden
                  className={cx(
                    "absolute top-2 bottom-2 -left-3 w-[3px] animate-fade rounded-r bg-primary",
                    ancestor && "hidden md:group-data-[collapsed]/sidebar:block",
                  )}
                />
              )}
              <Icon
                name={item.icon}
                size={17}
                filled={active}
                className={cx("shrink-0 transition-colors", active ? "text-primary-ink" : "text-muted group-hover:text-ink")}
              />
              <span className={FADE_LABEL}>{item.label}</span>
            </Link>
            {/* Mũi tên cuối hàng: chỉ mở, đóng nhánh; nằm đè lên hàng nên trông như một phần của mục, không phải nút thêm. */}
            {isParent && (
              <button type="button" aria-expanded={open} aria-label={`${open ? "Thu gọn" : "Mở"} ${item.label}`}
                title={open ? "Thu gọn" : "Mở"}
                onClick={() => setToggled((previous) => ({ ...previous, [item.href]: !open }))}
                className="absolute top-1/2 right-1 inline-flex size-7 -translate-y-1/2 items-center justify-center rounded-md text-muted transition-colors hover:bg-sidebar-hover hover:text-ink pointer-coarse:right-0 pointer-coarse:size-10 md:group-data-[collapsed]/sidebar:hidden">
                <Icon name="expand_more" size={16} className={cx("transition-transform duration-200", !open && "-rotate-90")} />
              </button>
            )}
            </div>
            {open && branch?.content}
          </Fragment>
        );
      })}
      </nav>}
      {collapsed && tip && (
        <span
          aria-hidden
          data-nav-tip
          style={{ top: tip.top }}
          className="pointer-events-none absolute left-full z-50 ml-2 hidden -translate-y-1/2 animate-fade rounded-lg bg-ink px-2.5 py-1.5 text-xs font-semibold whitespace-nowrap text-surface shadow-[var(--shadow-float)] md:block"
        >
          {tip.label}
        </span>
      )}
    </div>
  );
}

const SLIDE = "[transform-box:view-box] transition-transform duration-300 ease-[cubic-bezier(.16,1,.3,1)] motion-reduce:transition-none";

/**
 * Khung cửa sổ có ô sidebar bên trái, bật tắt như công tắc: sidebar mở thì ô trái tô kín và vạch ngăn đứng xa; thu gọn thì
 * ô co lại thành một dải hẹp và vạch ngăn trượt về sát mép.
 */
function ToggleIcon({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 20 20" width={20} height={20} aria-hidden="true" fill="none" stroke="currentColor" strokeWidth={1.7}>
      <rect x="2.5" y="3.5" width="15" height="13" rx="3.2" />
      <rect x="4.6" y="5.6" width="4" height="8.8" rx="1.3" fill="currentColor" stroke="none" className={cx(SLIDE, "origin-[4.6px_10px]", open ? "scale-x-100 opacity-100" : "scale-x-[.4] opacity-60")} />
      <path d="M10.6 3.8v12.4" className={cx(SLIDE, open ? "translate-x-0" : "-translate-x-[3.6px]")} />
    </svg>
  );
}

function AccountLink({ href, icon, label }: { href: string; icon?: IconName; label: string }) {
  return (
    <Link
      href={href}
      role="menuitem"
      data-menu-item
      className="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] text-ink transition-colors hover:bg-sunken"
    >
      {icon && <Icon name={icon} size={16} className="text-muted" />}
      {label}
    </Link>
  );
}
