import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// Khung chung của mọi màn /admin/* (Admin Dashboard, Settings, Account List, Subject List, Audit Log):
// U01 kiểm phiên và role ADMIN rồi vẽ shared/layout/AppShell với menu bên của Admin.
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["ADMIN"]}>{children}</AuthenticatedLayout>;
}
