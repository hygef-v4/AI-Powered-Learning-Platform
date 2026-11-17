"use client";

import { useRouter } from "next/navigation";
import { completeActivation } from "../../api";
import { InfoCard } from "../../components/InfoCard";
import { OtpFlow, type OtpFlowCopy } from "../components/OtpFlow";
import { msg } from "@/i18n/msg";

// Chữ theo design `c02a`, `c02b`, `c02`.
const COPY: OtpFlowCopy = {
  purpose: "ACTIVATION",
  title: () => msg("identity.otp.pages.ActivationPage.activateAccount"),
  steps: ["Email", msg("identity.otp.pages.ActivationPage.otpCode"), msg("identity.otp.pages.ActivationPage.setPassword")],
  sendLabel: msg("identity.otp.pages.ActivationPage.sendActivationCode"),
  codeLabel: msg("identity.otp.pages.ActivationPage.activationCode"),
  submitLabel: msg("identity.otp.pages.ActivationPage.activateAndSignIn"),
  aside: () => (
    <InfoCard
      title={msg("identity.otp.pages.ActivationPage.firstTimeHere")}
      subtitle={msg("identity.otp.pages.ActivationPage.howToActivateYourAccount")}
      points={[msg("identity.otp.pages.ActivationPage.useTheSchoolEmailIssued"), msg("identity.otp.pages.ActivationPage.theOtpCodeIsValid")]}
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
