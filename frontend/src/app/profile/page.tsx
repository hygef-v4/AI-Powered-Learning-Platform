import type { Metadata } from "next";
import { ProfilePage } from "@/features/identity/profile/pages/ProfilePage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.profile.page.hoSo") };
}

export default function Page() {
  return <ProfilePage />;
}
