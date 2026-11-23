import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
import { NO_INDEX } from "@/shared/siteMetadata";

// Trang cần đăng nhập: không cho máy tìm kiếm index.
export const metadata: Metadata = { robots: NO_INDEX };

// U11: Quiz Practice chỉ cho Student (BR-U11-01). U01 chặn phiên và role; backend vẫn kiểm quyền thật.
export default function QuizPracticeLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT"]}>{children}</AuthenticatedLayout>;
}
