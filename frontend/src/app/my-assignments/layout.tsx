import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U08: Student Assignments, Assignment Detail, lịch sử nộp; chỉ Student. Backend vẫn kiểm quyền thật.
export default function MyAssignmentsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT"]}>{children}</AuthenticatedLayout>;
}
