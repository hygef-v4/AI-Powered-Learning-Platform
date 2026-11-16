import type { Metadata } from "next";
import { ActivationPage } from "@/features/identity/otp/pages/ActivationPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.auth.activate.page.kichHoatTaiKhoan") };
}

export default function Page() {
  return <ActivationPage />;
}
