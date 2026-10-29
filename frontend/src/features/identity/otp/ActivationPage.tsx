"use client";

import { useRouter } from "next/navigation";
import { completeActivation } from "../api";
import { InfoCard } from "../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "./OtpFlow";

// Chữ theo design `c02a`, `c02b`, `c02`.
const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: () => "Kích hoạt tài khoản",
  steps: ["Email", "Mã OTP", "Đặt mật khẩu"],
  sendLabel: "Gửi mã kích hoạt",
  codeLabel: "Mã kích hoạt",
  submitLabel: "Kích hoạt và đăng nhập",
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
