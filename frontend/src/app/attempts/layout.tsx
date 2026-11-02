import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U08: Assignment Workspace, biên nhận, xem sơ đồ của bài làm; chỉ Student. Backend vẫn kiểm quyền thật.
export default function AttemptsLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["STUDENT"]}>{children}</AuthenticatedLayout>;
}
