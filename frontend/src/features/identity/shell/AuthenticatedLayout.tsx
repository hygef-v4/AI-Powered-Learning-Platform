"use client";

import type { ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { AppShell } from "@/shared/layout/AppShell";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { SessionProvider, useSession } from "../session";
import { accountMenuEntries, headerWidgets, navEntries } from "../slots";
import { RoleGuard } from "./RoleGuard";

/**
 * Bọc route cần đăng nhập: tải phiên, chặn role (BR-U01-48), rồi vẽ khung chung `shared/layout/AppShell`
 * với menu bên (`NavEntry`) và lối vào thanh trên (`AccountMenuEntry`) theo role thật của phiên.
 */
export function AuthenticatedLayout({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  return (
    <SessionProvider>
      <RoleGuard roles={roles}>
        <SessionShell>{children}</SessionShell>
      </RoleGuard>
    </SessionProvider>
  );
}

function SessionShell({ children }: { children: ReactNode }) {
  const session = useSession();
  if (session.status !== "authenticated") return null;
  const { role, displayName, email } = session.profile;
  const nav = navEntries
    .entries()
    .filter((entry) => entry.roles.includes(role))
    .map(({ href, title, icon }) => ({ href, label: title, icon }));
  const links = accountMenuEntries
    .entries()
    .filter((entry) => entry.roles.includes(role))
    .map(({ href, title, icon }) => ({ href, label: title, icon }));
  return (
    <AppShell
      role={role}
      nav={nav}
      headerLinks={links}
      headerWidgets={<SlotOutlet slot={headerWidgets} props={{}} />}
      user={{ displayName, email }}
    >
      {children}
    </AppShell>
  );
}
