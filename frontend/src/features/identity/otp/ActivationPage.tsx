"use client";

import { useRouter } from "next/navigation";
import { completeActivation } from "../api";
import { AuthLayout } from "../components/AuthLayout";
import { InfoCard } from "../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "./OtpFlow";

const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: "Kích hoạt tài khoản",
  subtitle: "Tài khoản do trường cấp. Xác minh email rồi đặt mật khẩu.",
  passwordTitle: "Kích hoạt tài khoản",
  passwordSubtitle: () => "Tài khoản do trường cấp. Xác minh email rồi đặt mật khẩu.",
  steps: ["Email", "Mã OTP", "Đặt mật khẩu"],
  submitLabel: "Kích hoạt và đăng nhập",
  submitNote: "Sau khi kích hoạt, bạn sẽ vào thẳng trang của vai trò mình.",
};

/** Màn Activate Account (UC 01). Kích hoạt xong backend đã tạo phiên nên vào thẳng homePath (BR-U01-13, 48). */
export function ActivationPage() {
  const router = useRouter();
  return (
    <AuthLayout
      aside={
        <InfoCard
          title="Lần đầu sử dụng?"
          subtitle="Hướng dẫn kích hoạt tài khoản"
          points={["Dùng email trường đã được cấp.", "Mã OTP có hiệu lực 10 phút."]}
        />
      }
    >
      <OtpFlow
        copy={COPY}
        onComplete={async (ticket, password) => {
          const session = await completeActivation(ticket, password);
          router.replace(session.homePath);
        }}
      />
    </AuthLayout>
  );
}
