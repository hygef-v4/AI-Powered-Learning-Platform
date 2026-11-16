import type { Metadata } from "next";
import { Suspense } from "react";
import { AccountListPage } from "@/features/identity/admin/pages/AccountListPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.accounts.page.taiKhoan") };
}

// AccountListPage đọc bộ lọc từ URL (useSearchParams) nên cần Suspense.
export default function Page() {
  return (
    <Suspense>
      <AccountListPage />
    </Suspense>
  );
}
