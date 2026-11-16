import type { Metadata } from "next";
import { SettingsSectionHost } from "@/features/settings/components/SettingsSectionHost";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.settings.sections.sectionKey.page.caiDat") };
}

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
