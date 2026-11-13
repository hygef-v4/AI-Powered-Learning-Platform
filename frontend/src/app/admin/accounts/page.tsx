import type { Metadata } from "next";
import { Suspense } from "react";
import { AccountListPage } from "@/features/identity/admin/pages/AccountListPage";

export const metadata: Metadata = { title: "Tài khoản" };

// AccountListPage đọc bộ lọc từ URL (useSearchParams) nên cần Suspense.
export default function Page() {
  return (
    <Suspense>
      <AccountListPage />
    </Suspense>
  );
}
