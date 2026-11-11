import type { Metadata } from "next";
import { PasswordResetPage } from "@/features/identity/otp/pages/PasswordResetPage";

export const metadata: Metadata = { title: "Khôi phục mật khẩu" };

export default function Page() {
  return <PasswordResetPage />;
}
