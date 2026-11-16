import type { Metadata } from "next";
import { SettingListPage } from "@/features/settings/pages/SettingListPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.settings.page.caiDat") };
}

export default function Page() {
  return <SettingListPage />;
}
