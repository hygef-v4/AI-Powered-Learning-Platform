import { Suspense } from "react";
import { LoginPage } from "@/features/identity/pages/LoginPage";

// LoginPage đọc ?reset=1 bằng useSearchParams nên cần Suspense để trang vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <LoginPage />
    </Suspense>
  );
}
