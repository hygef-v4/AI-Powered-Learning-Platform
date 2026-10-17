"use client";

import { useRouter } from "next/navigation";
import { completePasswordReset } from "../api";
import { AuthLayout } from "../components/AuthLayout";
import { InfoCard } from "../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "./OtpFlow";

const COPY: OtpFlowCopy = {
  purpose: "PASSWORD_RESET",
  title: "Khôi phục mật khẩu",
  subtitle: "Nhập email trường, mã OTP và mật khẩu mới.",
  passwordTitle: "Đặt lại mật khẩu",
  passwordSubtitle: (email) => `Mã OTP đã được xác nhận. Đặt mật khẩu mới cho ${email}.`,
  steps: ["Email", "Mã OTP", "Mật khẩu mới"],
  submitLabel: "Đặt lại mật khẩu",
  // BR-U01-34: đặt lại qua OTP xóa mọi phiên (Q12).
  submitNote: "Mọi thiết bị đang đăng nhập sẽ bị đăng xuất.",
};

/** Màn Password Reset (UC 04). Không tự đăng nhập: xong thì về User Login kèm thông báo. */
export function PasswordResetPage() {
  const router = useRouter();
  return (
    <AuthLayout
      aside={
        <InfoCard
          title="Bảo mật tài khoản"
          subtitle="Lưu ý khi đặt lại mật khẩu"
          points={["Mọi thiết bị đang đăng nhập sẽ bị đăng xuất.", "Đăng nhập lại bằng mật khẩu mới sau khi hoàn tất."]}
        />
      }
    >
      <OtpFlow
        copy={COPY}
        onComplete={async (ticket, password) => {
          await completePasswordReset(ticket, password);
          router.replace("/login?reset=1");
        }}
      />
    </AuthLayout>
  );
}
