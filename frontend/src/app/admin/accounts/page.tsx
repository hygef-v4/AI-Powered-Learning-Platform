import { Suspense } from "react";
import { AccountListPage } from "@/features/identity/admin/pages/AccountListPage";

// AccountListPage đọc bộ lọc từ URL (useSearchParams) nên cần Suspense.
export default function Page() {
  return (
    <Suspense>
      <AccountListPage />
    </Suspense>
  );
}
