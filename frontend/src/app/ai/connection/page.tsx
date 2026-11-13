import type { Metadata } from "next";
import { AiConnectionPage } from "@/features/ai";

export const metadata: Metadata = { title: "Kết nối AI" };

export default async function Page({ searchParams }: { searchParams: Promise<{ tab?: string | string[] }> }) {
  const { tab } = await searchParams;
  return <AiConnectionPage initialTab={typeof tab === "string" ? tab : undefined} />;
}
