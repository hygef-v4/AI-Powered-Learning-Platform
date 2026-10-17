import type { ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { SessionProvider } from "../session";
import { AppShell } from "./AppShell";
import { RoleGuard } from "./RoleGuard";

/** Bọc các route cần đăng nhập: tải phiên, chặn role, vẽ khung sau đăng nhập. Unit khác dùng trong layout của route mình. */
export function AuthenticatedLayout({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  return (
    <SessionProvider>
      <RoleGuard roles={roles}>
        <AppShell>{children}</AppShell>
      </RoleGuard>
    </SessionProvider>
  );
}
