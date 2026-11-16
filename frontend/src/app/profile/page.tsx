import type { Metadata } from "next";
import { ProfilePage } from "@/features/identity/profile/pages/ProfilePage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.profile.page.hoSo") };
}

export default function Page() {
  return <ProfilePage />;
}
