import { SettingsSectionHost } from "@/features/settings/SettingsSectionHost";

export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ sectionKey: string }>;
  searchParams: Promise<{ tab?: string | string[] }>;
}) {
  const { sectionKey } = await params;
  const { tab } = await searchParams;
  return <SettingsSectionHost sectionKey={decodeURIComponent(sectionKey)} tab={typeof tab === "string" ? tab : undefined} />;
}
