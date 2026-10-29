import { Suspense } from "react";
import { AuditLogPage } from "@/features/audit/AuditLogPage";

export default function Page() {
  return (
    <Suspense>
      <AuditLogPage />
    </Suspense>
  );
}
