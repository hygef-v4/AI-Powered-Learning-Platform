// U05 khai báo; U07–U14 đăng ký vào. Xem domain-entities U05, mục Contract frontend.

import type { Role } from "@/shared/auth/roles";
import { createRegistry, createSlot } from "@/shared/slots/registry";

/** Phần nội dung trên Student Class Detail (U08, U12, U13, U14). */
export const studentClassSections = createSlot<{ classId: string }>("StudentClassSection");

/** Tab hoặc phần trên Teacher Class Detail (U07, U09, U10, U12, U13). */
export const teacherClassTabs = createSlot<{ classId: string; tab: "students" | "evals" | "materials" }>(
  "TeacherClassTab",
);

/** Phần trong popup Student Detail (U12). */
export const studentDetailSections = createSlot<{ classId: string; studentId: string }>("StudentDetailSection");

/** Lối vào màn của unit khác trên Class Dashboard và Manager Dashboard. */
export type DashboardNavEntry = {
  key: string;
  order?: number;
  dashboard: "class" | "manager";
  title: string;
  href: string;
  roles: Role[];
};

export const dashboardNavEntries = createRegistry<DashboardNavEntry>("DashboardNavEntry");
