import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U12: workspace tài liệu nhóm cho sinh viên và giảng viên của lớp (backend kiểm thành viên và lớp).
export default function GroupDocsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
