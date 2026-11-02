import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U05: lớp được giao dạy, cho Teacher và Subject Manager.
// U01 chặn phiên và role rồi vẽ khung chung AppShell (menu bên theo role của phiên, hồ sơ, đăng xuất).
export default function TeachingLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
