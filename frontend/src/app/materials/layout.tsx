import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };
export default function Layout({ children }: { children: ReactNode }) {
  return (
    <AuthenticatedLayout roles={["SUBJECT_MANAGER", "TEACHER", "STUDENT"]}>
      {children}
    </AuthenticatedLayout>
  );
}
