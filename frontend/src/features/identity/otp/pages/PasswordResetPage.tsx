"use client";

import { useRouter } from "next/navigation";
import { completePasswordReset } from "../../api";
import { InfoCard } from "../../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "../components/OtpFlow";
import { msg } from "@/i18n/msg";

// Chữ theo design `c03a`, `c03`, `c04`.
const COPY: OtpFlowCopy = {
  purpose: "PASSWORD_RESET",
  title: (step) => (step === 2 ? msg("identity.otp.pages.PasswordResetPage.datLaiMatKhau") : msg("identity.otp.pages.PasswordResetPage.khoiPhucMatKhau")),
  steps: ["Email", msg("identity.otp.pages.PasswordResetPage.maOtp"), msg("identity.otp.pages.PasswordResetPage.matKhauMoi")],
  sendLabel: msg("identity.otp.pages.PasswordResetPage.guiMaKhoiPhuc"),
  codeLabel: msg("identity.otp.pages.PasswordResetPage.maXacThucOtp"),
  submitLabel: msg("identity.otp.pages.PasswordResetPage.datLaiMatKhau"),
  aside: (step) =>
    step === 2 ? (
      <InfoCard
        title={msg("identity.otp.pages.PasswordResetPage.baoMatTaiKhoan")}
        subtitle={msg("identity.otp.pages.PasswordResetPage.luuYKhiDatLaiMat")}
        points={[msg("identity.otp.pages.PasswordResetPage.dangNhapLaiBangMatKhau")]}
      />
    ) : (
      <InfoCard
        title={msg("identity.otp.pages.PasswordResetPage.baoMatTaiKhoan")}
        subtitle={msg("identity.otp.pages.PasswordResetPage.chinhSachBaoVeTaiKhoan")}
        points={[msg("identity.otp.pages.PasswordResetPage.khoiPhucSeDangXuatMoi"), msg("identity.otp.pages.PasswordResetPage.dangNhapLaiBangMatKhau")]}
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
