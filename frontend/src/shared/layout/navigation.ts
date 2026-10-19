// Nhãn role, màu avatar và cách chọn mục đang mở cho khung trang. Mục menu bên do unit đăng ký qua NavEntry (U01).

import type { Role } from "@/shared/auth/roles";
import type { IconName } from "@/shared/ui/Icon";
import type { Tone } from "@/shared/ui/tones";

/** `section`: tiêu đề nhóm vẽ phía trên mục đầu tiên của nhóm (ví dụ "Giảng dạy" của Chủ nhiệm môn). */
export type NavItem = { href: string; label: string; icon: IconName; section?: string };

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
