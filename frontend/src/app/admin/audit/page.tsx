import type { Metadata } from "next";
import { Suspense } from "react";
import { AuditLogPage } from "@/features/audit/pages/AuditLogPage";

export const metadata: Metadata = { title: "Nhật ký audit" };

export default function Page() {
  return (
    <Suspense>
      <AuditLogPage />
    </Suspense>
  );
}
