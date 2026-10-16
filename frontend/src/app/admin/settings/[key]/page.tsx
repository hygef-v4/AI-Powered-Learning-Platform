import { SettingDetailPage } from "@/features/settings/SettingDetailPage";

export default async function Page({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  return <SettingDetailPage settingKey={decodeURIComponent(key)} />;
}
