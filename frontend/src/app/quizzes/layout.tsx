import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U10: Quiz Detail cho giảng viên lớp và Chủ nhiệm môn. U01 chặn phiên và role; backend vẫn kiểm quyền thật.
export default function QuizzesLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
