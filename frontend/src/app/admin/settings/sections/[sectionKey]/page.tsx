import type { Metadata } from "next";
import { SettingsSectionHost } from "@/features/settings/components/SettingsSectionHost";

export const metadata: Metadata = { title: "Cài đặt" };

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
