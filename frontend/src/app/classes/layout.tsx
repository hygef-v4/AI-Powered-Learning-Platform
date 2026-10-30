import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U05: lớp của tôi (Student) và Class Dashboard; Teacher, Subject Manager vào đây theo trang chủ của U01.
// U01 chặn phiên và role rồi vẽ khung chung AppShell (menu bên theo role của phiên, hồ sơ, đăng xuất).
export default function ClassesLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
