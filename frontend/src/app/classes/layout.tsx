import type { ReactNode } from "react";
import { SessionProvider } from "@/features/identity/session";
import { RoleGuard } from "@/features/identity/shell/RoleGuard";

// U05: lớp của tôi (Student) và Class Dashboard; Teacher, Subject Manager vào đây theo trang chủ của U01.
// Chỉ chặn phiên và role; khung trang do AcademicsShell của U05 vẽ, đổi sang AuthenticatedLayout khi U05 dùng AppShell.
export default function ClassesLayout({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <RoleGuard roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</RoleGuard>
    </SessionProvider>
  );
}
