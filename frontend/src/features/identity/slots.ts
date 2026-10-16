// U01 khai báo; unit sau đăng ký vào. Xem domain-entities U01, mục Contract frontend.

import type { Role } from "@/shared/auth/roles";
import { createRegistry, createSlot } from "@/shared/slots/registry";

/** Mục thêm vào menu tài khoản (U06: AI Connection). */
export type AccountMenuEntry = {
  key: string;
  order?: number;
  title: string;
  href: string;
  roles: Role[];
};

export const accountMenuEntries = createRegistry<AccountMenuEntry>("AccountMenuEntry");

/** Thành phần trên thanh điều hướng cho mọi vai trò (U14: chuông thông báo). */
export const headerWidgets = createSlot<Record<string, never>>("HeaderWidget");
