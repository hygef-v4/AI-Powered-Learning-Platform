import type { Metadata } from "next";
import { ActivationPage } from "@/features/identity/otp/pages/ActivationPage";
import { translateOnServer } from "@/i18n/server";
import { NO_INDEX } from "@/shared/siteMetadata";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.auth.activate.page.activateAccount"), robots: NO_INDEX };
}

export default function Page() {
  return <ActivationPage />;
}
