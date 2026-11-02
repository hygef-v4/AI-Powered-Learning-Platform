import { Suspense } from "react";
import { AuditLogPage } from "@/features/audit/pages/AuditLogPage";

export default function Page() {
  return (
    <Suspense>
      <AuditLogPage />
    </Suspense>
  );
}
