import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U07: Assignment Form cho Teacher và Subject Manager; backend vẫn kiểm quyền thật trên từng bài.
export default function AssignmentsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
