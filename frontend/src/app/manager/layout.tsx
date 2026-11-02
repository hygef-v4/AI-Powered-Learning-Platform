import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U05: môn và lớp của Chủ nhiệm môn.
// U01 chặn phiên và role rồi vẽ khung chung AppShell (menu bên theo role của phiên, hồ sơ, đăng xuất).
export default function ManagerLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
