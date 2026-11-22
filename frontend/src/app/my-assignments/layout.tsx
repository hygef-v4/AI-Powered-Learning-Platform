import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U08: Student Assignments, Assignment Detail, lịch sử nộp; chỉ Student. Backend vẫn kiểm quyền thật.
export default function MyAssignmentsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT"]}>{children}</AuthenticatedLayout>;
}
