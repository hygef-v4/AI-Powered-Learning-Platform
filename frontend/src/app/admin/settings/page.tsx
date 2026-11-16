import type { Metadata } from "next";
import { SettingListPage } from "@/features/settings/pages/SettingListPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.settings.page.caiDat") };
}

export default function Page() {
  return <SettingListPage />;
}
