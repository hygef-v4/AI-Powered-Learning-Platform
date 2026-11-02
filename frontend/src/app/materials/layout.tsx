import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/components/AuthenticatedLayout";
export default function Layout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout roles={["SUBJECT_MANAGER", "TEACHER", "STUDENT"]}>{children}</AuthenticatedLayout>;
}
