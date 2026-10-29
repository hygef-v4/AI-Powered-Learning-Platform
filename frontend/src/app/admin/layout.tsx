import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// Khung chung của mọi màn /admin/* (Admin Dashboard, Settings, Account List, Subject List, Audit Log):
// U01 kiểm phiên và role ADMIN rồi vẽ shared/layout/AppShell với menu bên của Admin.
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["ADMIN"]}>{children}</AuthenticatedLayout>;
}
