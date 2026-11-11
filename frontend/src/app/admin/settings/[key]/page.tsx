import type { Metadata } from "next";
import { SettingDetailPage } from "@/features/settings/pages/SettingDetailPage";

export const metadata: Metadata = { title: "Cài đặt" };

export default async function Page({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  return <SettingDetailPage settingKey={decodeURIComponent(key)} />;
}
