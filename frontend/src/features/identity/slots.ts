// U01 khai báo; unit sau đăng ký vào. Xem domain-entities U01, mục Contract frontend.

import type { Role } from "@/shared/auth/roles";
import type { NavContext, NavLocation } from "@/shared/layout/navigation";
import { createRegistry, createSlot } from "@/shared/slots/registry";
import type { IconName } from "@/shared/ui/Icon";

/** Lối vào trên thanh trên cùng, cạnh hồ sơ (U06: Kết nối AI). */
export type AccountMenuEntry = {
  key: string;
  order?: number;
  title: string;
  href: string;
  roles: Role[];
  icon?: IconName;
};

export const accountMenuEntries = createRegistry<AccountMenuEntry>("AccountMenuEntry");

/** Mục menu bên theo role; unit sở hữu màn tự đăng ký mục của mình (bảng menu ở frontend-components U01). */
export type NavEntry = {
  key: string;
  order?: number;
  title: string;
  href: string;
  icon: IconName;
  roles: Role[];
  /** Tiêu đề nhóm trên menu bên; các mục cùng nhóm đứng liền nhau theo `order`. */
  section?: string;
};

export const navEntries = createRegistry<NavEntry>("NavEntry");

/**
 * Sidebar theo ngữ cảnh (một môn, một lớp) thay menu của role khi `resolve` khớp trang đang mở; unit sở hữu ngữ cảnh tự
 * đăng ký. Mục đầu tiên khớp (theo `order`) thắng.
 */
export type ContextNavEntry = {
  key: string;
  order?: number;
  roles: Role[];
  resolve: (at: NavLocation) => NavContext | null;
};

export const contextNavEntries = createRegistry<ContextNavEntry>("ContextNavEntry");

/**
 * Mục trên sidebar của một môn (Chủ nhiệm môn); unit sở hữu màn tự đăng ký. `path` là trang của mục, link mang
 * `?subjectId=` của môn đang mở; `href` đổi cách dựng link (ví dụ Thông tin môn là `/manager/subjects/{id}`).
 */
export type SubjectNavEntry = {
  key: string;
  order?: number;
  title: string;
  icon: IconName;
  path: string;
  href?: (subjectId: string) => string;
};

export const subjectNavEntries = createRegistry<SubjectNavEntry>("SubjectNavEntry");

/** Thành phần trên thanh điều hướng cho mọi vai trò (U14: chuông thông báo). */
export const headerWidgets = createSlot<Record<string, never>>("HeaderWidget");
