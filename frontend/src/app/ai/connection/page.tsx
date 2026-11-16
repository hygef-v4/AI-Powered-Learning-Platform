import type { Metadata } from "next";
import { AiConnectionPage } from "@/features/ai";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.ai.connection.page.aiConnection") };
}

export default async function Page({ searchParams }: { searchParams: Promise<{ tab?: string | string[] }> }) {
  const { tab } = await searchParams;
  return <AiConnectionPage initialTab={typeof tab === "string" ? tab : undefined} />;
}
