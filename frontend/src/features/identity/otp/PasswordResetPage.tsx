"use client";

import { useRouter } from "next/navigation";
import { completePasswordReset } from "../api";
import { InfoCard } from "../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "./OtpFlow";

// Chữ theo design `c03a`, `c03`, `c04`.
const COPY: OtpFlowCopy = {
  purpose: "PASSWORD_RESET",
  title: (step) => (step === 2 ? "Đặt lại mật khẩu" : "Khôi phục mật khẩu"),
  subtitle: (step, email) =>
    step === 0
      ? "Nhập email trường để nhận mã khôi phục."
      : step === 1
        ? "Nhập email trường, mã OTP và mật khẩu mới."
        : `Mã OTP đã được xác nhận. Đặt mật khẩu mới cho ${email}.`,
  steps: ["Email", "Mã OTP", "Mật khẩu mới"],
  sendLabel: "Gửi mã khôi phục",
  sendNote: "Sau khi gửi, hệ thống luôn hiện cùng một thông báo và không cho biết email có tồn tại hay không.",
  codeLabel: "Mã xác thực OTP (6 chữ số)",
  verifyNote: "Nếu email thuộc một tài khoản của trường, mã khôi phục đã được gửi tới email đó.",
  submitLabel: "Đặt lại mật khẩu",
  // Đặt lại qua OTP xóa mọi phiên (BR-U01-34); thiết bị đang đặt lại chưa đăng nhập nên chỉ còn "các thiết bị khác".
  submitNote: "Sau khi đặt lại, các thiết bị khác sẽ bị đăng xuất.",
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
