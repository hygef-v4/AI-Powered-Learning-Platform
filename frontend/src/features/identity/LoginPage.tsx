"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { Alert } from "@/shared/ui/Alert";
import { getProfile } from "./api";
import { AuthLayout } from "./components/AuthLayout";
import { InfoCard } from "./components/InfoCard";
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
    <AuthLayout
      variant="login"
      aside={
        <InfoCard
          title="Lần đầu sử dụng?"
          subtitle="Tài khoản do Phòng Đào tạo cấp"
          text="Kích hoạt bằng email trường và mã OTP. Quên mật khẩu thì khôi phục bằng mã gửi về email."
        />
      }
    >
      {justReset && (
        <Alert tone="success" role="status" className="mb-6">
          Đã đặt lại mật khẩu. Các thiết bị khác đã bị đăng xuất; hãy đăng nhập bằng mật khẩu mới.
        </Alert>
      )}
      <LoginForm onSuccess={(session) => router.replace(session.homePath)} />
    </AuthLayout>
  );
}
