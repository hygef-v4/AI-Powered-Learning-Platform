"use client";

import type { ReactNode } from "react";
import type { Role } from "@/shared/auth/roles";
import { AppShell } from "@/shared/layout/AppShell";
import type { NavLocation } from "@/shared/layout/navigation";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { SessionProvider, useSession } from "../../hooks/session";
import { accountMenuEntries, contextNavEntries, headerWidgets, navEntries } from "../../slots";
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
    .map(({ href, title, icon, section }) => ({ href, label: title, icon, section }));
  const links = accountMenuEntries
    .entries()
    .filter((entry) => entry.roles.includes(role))
    .map(({ href, title, icon }) => ({ href, label: title, icon }));
  const contexts = contextNavEntries.entries().filter((entry) => entry.roles.includes(role));
  const context = (at: NavLocation) => {
    for (const entry of contexts) {
      const found = entry.resolve(at);
      if (found) return found;
    }
    return null;
  };
  return (
    <AppShell
      role={role}
      nav={nav}
      context={contexts.length > 0 ? context : undefined}
      headerLinks={links}
      headerWidgets={<SlotOutlet slot={headerWidgets} props={{}} />}
      user={{ displayName, email }}
    >
      {children}
    </AppShell>
  );
}
