import type { Metadata } from "next";
import { ActivationPage } from "@/features/identity/otp/pages/ActivationPage";

export const metadata: Metadata = { title: "Kích hoạt tài khoản" };

export default function Page() {
  return <ActivationPage />;
}
