import type { Metadata } from "next";
import { Suspense } from "react";
import { AuditLogPage } from "@/features/audit/pages/AuditLogPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.audit.page.nhatKyAudit") };
}

export default function Page() {
  return (
    <Suspense>
      <AuditLogPage />
    </Suspense>
  );
}
