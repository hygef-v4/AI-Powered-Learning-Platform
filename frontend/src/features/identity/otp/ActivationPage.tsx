"use client";

import { useRouter } from "next/navigation";
import { completeActivation } from "../api";
import { InfoCard } from "../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "./OtpFlow";

// Chữ theo design `c02a`, `c02b`, `c02`.
const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: () => "Kích hoạt tài khoản",
  subtitle: (step) =>
    step === 0
      ? "Tài khoản do trường cấp. Nhập email trường để nhận mã kích hoạt."
      : step === 1
        ? "Nhập mã 6 chữ số vừa được gửi tới email trường."
        : "Tài khoản do trường cấp. Xác minh email rồi đặt mật khẩu.",
  steps: ["Email", "Mã OTP", "Đặt mật khẩu"],
  sendLabel: "Gửi mã kích hoạt",
  sendNote: "Mã gồm 6 chữ số, hiệu lực 10 phút. Nếu chưa nhận được, bạn có thể gửi lại sau 60 giây.",
  codeLabel: "Mã kích hoạt (6 chữ số)",
  verifyNote: "Nhập sai quá 5 lần thì mã bị hủy và bạn cần gửi mã mới.",
  submitLabel: "Kích hoạt và đăng nhập",
  submitNote: "Sau khi kích hoạt, bạn sẽ vào thẳng trang của vai trò mình.",
  aside: () => (
    <InfoCard
      title="Lần đầu sử dụng?"
      subtitle="Hướng dẫn kích hoạt tài khoản"
      points={["Dùng email trường đã được cấp.", "Mã OTP có hiệu lực 10 phút."]}
    />
  ),
};

/** Màn Activate Account (UC 01). Kích hoạt xong backend đã tạo phiên nên vào thẳng homePath (BR-U01-13, 48). */
export function ActivationPage() {
  const router = useRouter();
  return (
    <OtpFlow
      copy={COPY}
      onComplete={async (ticket, password) => {
        const session = await completeActivation(ticket, password);
        router.replace(session.homePath);
      }}
    />
  );
}
