import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U12: workspace tài liệu nhóm cho sinh viên và giảng viên của lớp (backend kiểm thành viên và lớp).
export default function GroupDocsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
