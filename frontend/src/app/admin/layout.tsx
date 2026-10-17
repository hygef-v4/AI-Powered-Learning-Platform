import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U01: mọi màn /admin/* (kể cả Settings của U03) chỉ dành cho ADMIN và có khung sau đăng nhập.
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["ADMIN"]}>{children}</AuthenticatedLayout>;
}
