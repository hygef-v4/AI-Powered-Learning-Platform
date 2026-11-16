"use client";

import { useRouter } from "next/navigation";
import { completeActivation } from "../../api";
import { InfoCard } from "../../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "../components/OtpFlow";
import { msg } from "@/i18n/msg";

// Chữ theo design `c02a`, `c02b`, `c02`.
const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: () => msg("identity.otp.pages.ActivationPage.kichHoatTaiKhoan"),
  steps: ["Email", msg("identity.otp.pages.ActivationPage.maOtp"), msg("identity.otp.pages.ActivationPage.datMatKhau")],
  sendLabel: msg("identity.otp.pages.ActivationPage.guiMaKichHoat"),
  codeLabel: msg("identity.otp.pages.ActivationPage.maKichHoat"),
  submitLabel: msg("identity.otp.pages.ActivationPage.kichHoatVaDangNhap"),
  aside: () => (
    <InfoCard
      title={msg("identity.otp.pages.ActivationPage.lanDauSuDung")}
      subtitle={msg("identity.otp.pages.ActivationPage.huongDanKichHoatTaiKhoan")}
      points={[msg("identity.otp.pages.ActivationPage.dungEmailTruongDaDuocCap"), msg("identity.otp.pages.ActivationPage.maOtpCoHieuLuc10")]}
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
