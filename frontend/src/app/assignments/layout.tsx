import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U07: Assignment Form cho Teacher và Subject Manager; backend vẫn kiểm quyền thật trên từng bài.
export default function AssignmentsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
