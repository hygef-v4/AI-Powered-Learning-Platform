"use client";

import { useRouter } from "next/navigation";
import { completePasswordReset } from "../../api";
import { InfoCard } from "../../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "../components/OtpFlow";

// Chữ theo design `c03a`, `c03`, `c04`.
const COPY: OtpFlowCopy = {
  purpose: "PASSWORD_RESET",
  title: (step) => (step === 2 ? "Đặt lại mật khẩu" : "Khôi phục mật khẩu"),
  steps: ["Email", "Mã OTP", "Mật khẩu mới"],
  sendLabel: "Gửi mã khôi phục",
  codeLabel: "Mã xác thực OTP",
  submitLabel: "Đặt lại mật khẩu",
  aside: (step) =>
    step === 2 ? (
      <InfoCard
        title="Bảo mật tài khoản"
        subtitle="Lưu ý khi đặt lại mật khẩu"
        points={["Đăng nhập lại bằng mật khẩu mới sau khi hoàn tất."]}
      />
    ) : (
      <InfoCard
        title="Bảo mật tài khoản"
        subtitle="Chính sách bảo vệ tài khoản FPT"
        points={["Khôi phục sẽ đăng xuất mọi thiết bị đang hoạt động.", "Đăng nhập lại bằng mật khẩu mới sau khi hoàn tất."]}
      />
    ),
  tagline: (step) => step < 2,
};

/** Màn Password Reset (UC 04). Không tự đăng nhập: xong thì về User Login kèm thông báo. */
export function PasswordResetPage() {
  const router = useRouter();
  return (
    <OtpFlow
      copy={COPY}
      onComplete={async (ticket, password) => {
        await completePasswordReset(ticket, password);
        router.replace("/login?reset=1");
      }}
    />
  );
}
