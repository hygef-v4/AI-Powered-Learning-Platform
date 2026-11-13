import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginPage } from "@/features/identity/pages/LoginPage";

export const metadata: Metadata = { title: "Đăng nhập" };

// LoginPage đọc ?reset=1 bằng useSearchParams nên cần Suspense để trang vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <LoginPage />
    </Suspense>
  );
}
