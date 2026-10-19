import type { ReactNode } from "react";
import { SessionProvider } from "@/features/identity/session";
import { RoleGuard } from "@/features/identity/shell/RoleGuard";

// U05: lớp được giao dạy, cho Teacher và Subject Manager.
// Chỉ chặn phiên và role; khung trang do AcademicsShell của U05 vẽ, đổi sang AuthenticatedLayout khi U05 dùng AppShell.
export default function TeachingLayout({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <RoleGuard roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</RoleGuard>
    </SessionProvider>
  );
}
