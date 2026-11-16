"use client";

import { useRouter } from "next/navigation";
import { completePasswordReset } from "../../api";
import { InfoCard } from "../../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "../components/OtpFlow";
import { msg } from "@/i18n/msg";

// Chữ theo design `c03a`, `c03`, `c04`.
const COPY: OtpFlowCopy = {
  purpose: "PASSWORD_RESET",
  title: (step) => (step === 2 ? msg("identity.otp.pages.PasswordResetPage.resetPassword") : msg("identity.otp.pages.PasswordResetPage.recoverPassword")),
  steps: ["Email", msg("identity.otp.pages.PasswordResetPage.otpCode"), msg("identity.otp.pages.PasswordResetPage.newPassword")],
  sendLabel: msg("identity.otp.pages.PasswordResetPage.sendRecoveryCode"),
  codeLabel: msg("identity.otp.pages.PasswordResetPage.otpVerificationCode"),
  submitLabel: msg("identity.otp.pages.PasswordResetPage.resetPassword"),
  aside: (step) =>
    step === 2 ? (
      <InfoCard
        title={msg("identity.otp.pages.PasswordResetPage.accountSecurity")}
        subtitle={msg("identity.otp.pages.PasswordResetPage.notesOnResettingYourPassword")}
        points={[msg("identity.otp.pages.PasswordResetPage.signInAgainWithYour")]}
      />
    ) : (
      <InfoCard
        title={msg("identity.otp.pages.PasswordResetPage.accountSecurity")}
        subtitle={msg("identity.otp.pages.PasswordResetPage.fptAccountProtectionPolicy")}
        points={[msg("identity.otp.pages.PasswordResetPage.recoverySignsOutAllActive"), msg("identity.otp.pages.PasswordResetPage.signInAgainWithYour")]}
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
