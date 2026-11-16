import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginPage } from "@/features/identity/pages/LoginPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.auth.login.page.dangNhap") };
}

// LoginPage đọc ?reset=1 bằng useSearchParams nên cần Suspense để trang vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <LoginPage />
    </Suspense>
  );
}
