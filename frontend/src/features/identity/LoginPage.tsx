"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { clearCurrentUser } from "@/shared/auth/useCurrentUser";
import { getProfile } from "./api";
import { AuthLayout } from "./components/AuthLayout";
import { AuthToast, type AuthNotice } from "./components/AuthToast";
import { InfoCard } from "./components/InfoCard";
import { LoginForm } from "./LoginForm";
import { safeNextPath } from "./session";

/**
 * Màn User Login (UC 02). Đã đăng nhập rồi thì chuyển thẳng tới dashboard theo role (F15). Có `?next=` (bị đưa về đây
 * khi hết phiên) thì quay lại trang đó; role không được vào thì RoleGuard đưa về homePath.
 */
export function LoginPage() {
  const router = useRouter();
  const params = useSearchParams();
  // Vừa đặt lại mật khẩu (`?reset=1`): báo một câu ngắn rồi tự ẩn.
  const [notice, setNotice] = useState<AuthNotice | null>(() =>
    params.get("reset") === "1" ? { id: 1, text: "Đã đặt lại mật khẩu.", tone: "success" } : null,
  );
  const clearNotice = useCallback(() => setNotice(null), []);
  const next = safeNextPath(params.get("next"));

  useEffect(() => {
    getProfile()
      .then((profile) => router.replace(next ?? profile.homePath))
      .catch(() => undefined);
  }, [router, next]);

  return (
    <AuthLayout
      variant="login"
      toast={<AuthToast notice={notice} onDone={clearNotice} />}
      aside={
        <InfoCard
          title="Lần đầu sử dụng?"
          subtitle="Tài khoản do Phòng Đào tạo cấp"
          text="Kích hoạt bằng email trường và mã OTP. Quên mật khẩu thì khôi phục bằng mã gửi về email."
        />
      }
    >
      <LoginForm
        onSuccess={(session) => {
          // Hồ sơ nhớ trong AppShell có thể là của người dùng trước.
          clearCurrentUser();
          router.replace(next ?? session.homePath);
        }}
      />
    </AuthLayout>
  );
}
