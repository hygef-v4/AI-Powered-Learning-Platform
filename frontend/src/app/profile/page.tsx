import type { Metadata } from "next";
import { ProfilePage } from "@/features/identity/profile/pages/ProfilePage";

export const metadata: Metadata = { title: "Hồ sơ" };

export default function Page() {
  return <ProfilePage />;
}
