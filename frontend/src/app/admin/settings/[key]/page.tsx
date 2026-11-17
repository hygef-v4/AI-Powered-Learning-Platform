import type { Metadata } from "next";
import { SettingDetailPage } from "@/features/settings/pages/SettingDetailPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.settings.key.page.settings") };
}

export default async function Page({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  return <SettingDetailPage settingKey={decodeURIComponent(key)} />;
}
