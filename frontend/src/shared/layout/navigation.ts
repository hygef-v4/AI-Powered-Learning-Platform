// Nhãn role, màu avatar và cách chọn mục đang mở cho khung trang. Mục menu bên do unit đăng ký qua NavEntry (U01).

import type { ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import type { IconName } from "@/shared/ui/Icon";
import type { Tone } from "@/shared/ui/tones";

/**
 * `section`: tiêu đề nhóm vẽ phía trên mục đầu tiên của nhóm. `active`: sidebar theo ngữ cảnh tự quyết mục đang mở (khi
 * mục phân biệt bằng `?tab=`); bỏ trống thì so đường dẫn.
 */
export type NavItem = { href: string; label: string; icon: IconName; section?: string; active?: boolean };

/**
 * Chữ trong sidebar mờ đi khi thu gọn (chỉ màn rộng) thay vì biến mất: mục giữ nguyên chỗ, chữ bị cắt theo bề ngang đang
 * hẹp lại, trình đọc màn hình vẫn đọc được tên mục.
 */
export const FADE_LABEL = "transition-opacity duration-200 md:group-data-[collapsed]/sidebar:opacity-0";

/** Trang đang mở, đủ để sidebar theo ngữ cảnh nhận ra môn hay lớp. */
export type NavLocation = { pathname: string; search: URLSearchParams };

/**
 * Sidebar theo ngữ cảnh (một môn, một lớp): thay menu của role khi đang ở trang của ngữ cảnh đó. `header` là lối về và
 * tên môn, lớp; `key` đổi thì menu mờ vào lại.
 */
export type NavContext = { key: string; label: string; header: ReactNode; items: NavItem[] };

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: "Sinh viên",
  TEACHER: "Giảng viên",
  SUBJECT_MANAGER: "Chủ nhiệm môn",
  ADMIN: "Quản trị viên",
};

export const AVATAR_TONES: Record<Role, Tone> = {
  STUDENT: "primary",
  TEACHER: "success",
  SUBJECT_MANAGER: "accent",
  ADMIN: "orange",
};

/** Mục đang mở: đường dẫn dài nhất khớp đầu pathname. */
export function activeHref(items: NavItem[], pathname: string): string | null {
  let best: string | null = null;
  for (const { href } of items) {
    const matches = pathname === href || pathname.startsWith(`${href}/`);
    if (matches && (best === null || href.length > best.length)) best = href;
  }
  return best;
}

/** Chữ viết tắt trên avatar: chữ đầu của hai từ cuối (tên gọi), một từ thì hai chữ đầu. */
export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "?";
  if (words.length === 1) return words[0]!.slice(0, 2).toUpperCase();
  return words
    .slice(-2)
    .map((word) => word[0]!)
    .join("")
    .toUpperCase();
}
