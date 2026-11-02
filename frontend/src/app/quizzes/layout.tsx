import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U10: Quiz Detail cho giảng viên lớp và Chủ nhiệm môn. U01 chặn phiên và role; backend vẫn kiểm quyền thật.
export default function QuizzesLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>;
}
