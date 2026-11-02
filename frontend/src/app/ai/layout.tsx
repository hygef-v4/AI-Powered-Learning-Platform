import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U06: Kết nối AI và Lịch sử sử dụng AI, cho Student, Teacher, Subject Manager (Admin không có API cá nhân hay agent).
export default function AiLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
