import type { Metadata } from "next";
import { PasswordResetPage } from "@/features/identity/otp/pages/PasswordResetPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.auth.resetpassword.page.khoiPhucMatKhau") };
}

export default function Page() {
  return <PasswordResetPage />;
}
