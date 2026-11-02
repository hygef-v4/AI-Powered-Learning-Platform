import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";

// U11: Quiz Practice chỉ cho Student (BR-U11-01). U01 chặn phiên và role; backend vẫn kiểm quyền thật.
export default function QuizAttemptsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT"]}>{children}</AuthenticatedLayout>;
}
