import type { ReactNode } from "react";
import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";

// U01: User Profile mở cho mọi role đã đăng nhập.
export default function ProfileLayout({ children }: { children: ReactNode }) {
  return <AuthenticatedLayout>{children}</AuthenticatedLayout>;
}
