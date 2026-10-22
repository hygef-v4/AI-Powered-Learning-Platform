import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";
export default function Layout({ children }: { children: ReactNode }) { return <AuthenticatedLayout roles={["STUDENT", "TEACHER", "SUBJECT_MANAGER"]}>{children}</AuthenticatedLayout>; }
