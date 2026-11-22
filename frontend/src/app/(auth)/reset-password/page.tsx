import type { Metadata } from "next";
import { PasswordResetPage } from "@/features/identity/otp/pages/PasswordResetPage";
import { translateOnServer } from "@/i18n/server";
import { NO_INDEX } from "@/shared/siteMetadata";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.auth.resetpassword.page.resetPassword"), robots: NO_INDEX };
}

export default function Page() {
  return <PasswordResetPage />;
}
