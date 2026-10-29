import type { ReactNode } from "react";
import { accountMenuEntries, headerWidgets, navEntries } from "@/features/identity/slots";
import { AppShell } from "@/shared/layout/AppShell";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";

// Khung chung của mọi màn /admin/* (Admin Dashboard, Settings, Account List, Subject List, Audit Log).
export default function AdminLayout({ children }: { children: ReactNode }) {
  const nav = navEntries
    .entries()
    .filter((entry) => entry.roles.includes("ADMIN"))
    .map(({ href, title, icon }) => ({ href, label: title, icon }));
  const links = accountMenuEntries
    .entries()
    .filter((entry) => entry.roles.includes("ADMIN"))
    .map(({ href, title, icon }) => ({ href, label: title, icon }));
  return (
    <AppShell
      role="ADMIN"
      nav={nav}
      headerLinks={links}
      headerWidgets={<SlotOutlet slot={headerWidgets} props={{}} />}
    >
      {children}
    </AppShell>
  );
}
