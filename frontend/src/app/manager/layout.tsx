import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U05: môn và lớp của Chủ nhiệm môn.
// U01 chặn phiên và role rồi vẽ khung chung AppShell (menu bên theo role của phiên, hồ sơ, đăng xuất).
export default function ManagerLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
