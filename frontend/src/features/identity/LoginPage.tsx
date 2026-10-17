"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { getProfile } from "./api";
import { AuthLayout } from "./components/AuthLayout";
import { RecentWorkPreview } from "./components/RecentWorkPreview";
import { LoginForm } from "./LoginForm";

/** Màn User Login (UC 02). Đã đăng nhập rồi thì chuyển thẳng tới dashboard theo role (F15). */
export function LoginPage() {
  const router = useRouter();
  const justReset = useSearchParams().get("reset") === "1";

  useEffect(() => {
    getProfile()
      .then((profile) => router.replace(profile.homePath))
      .catch(() => undefined);
  }, [router]);

  return (
    <AuthLayout aside={<RecentWorkPreview />}>
      {justReset && (
        <p role="status" className="mb-6 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          Đã đặt lại mật khẩu. Hãy đăng nhập lại bằng mật khẩu mới.
        </p>
      )}
      <LoginForm onSuccess={(session) => router.replace(session.homePath)} />
    </AuthLayout>
  );
}
