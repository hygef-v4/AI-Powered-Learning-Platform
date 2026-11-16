import type { Metadata } from "next";
import { ActivationPage } from "@/features/identity/otp/pages/ActivationPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.auth.activate.page.kichHoatTaiKhoan") };
}

export default function Page() {
  return <ActivationPage />;
}
