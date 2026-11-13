import type { Metadata } from "next";
import { SettingListPage } from "@/features/settings/pages/SettingListPage";

export const metadata: Metadata = { title: "Cài đặt" };

export default function Page() {
  return <SettingListPage />;
}
