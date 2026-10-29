import { SettingsSectionHost } from "@/features/settings/SettingsSectionHost";

export default async function Page({ params }: { params: Promise<{ sectionKey: string }> }) {
  const { sectionKey } = await params;
  return <SettingsSectionHost sectionKey={decodeURIComponent(sectionKey)} />;
}
