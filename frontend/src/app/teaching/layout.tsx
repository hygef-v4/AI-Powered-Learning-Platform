import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };
import { GradingAiTaskTracker } from "@/features/grading/components/GradingAiTaskTracker";

// U05: lớp được giao dạy, cho Teacher và Subject Manager.
// U01 chặn phiên và role rồi vẽ khung chung AppShell (menu bên theo role của phiên, hồ sơ, đăng xuất).
export default function TeachingLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}><GradingAiTaskTracker />{children}</AuthenticatedLayout>;
}
