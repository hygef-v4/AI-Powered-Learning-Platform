import type { Metadata } from "next";
import { SettingDetailPage } from "@/features/settings/pages/SettingDetailPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.settings.key.page.caiDat") };
}

export default async function Page({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  return <SettingDetailPage settingKey={decodeURIComponent(key)} />;
}
