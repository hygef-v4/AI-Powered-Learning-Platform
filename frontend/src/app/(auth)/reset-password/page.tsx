import type { Metadata } from "next";
import { PasswordResetPage } from "@/features/identity/otp/pages/PasswordResetPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.auth.resetpassword.page.khoiPhucMatKhau") };
}

export default function Page() {
  return <PasswordResetPage />;
}
