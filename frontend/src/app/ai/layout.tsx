import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U06: Kết nối AI và Lịch sử sử dụng AI, cho Student, Teacher, Subject Manager (Admin không có API cá nhân hay agent).
export default function AiLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
