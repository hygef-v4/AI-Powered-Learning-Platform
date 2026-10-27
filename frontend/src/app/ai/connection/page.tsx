import { AiConnectionPage } from "@/features/ai";

export default async function Page({ searchParams }: { searchParams: Promise<{ tab?: string | string[] }> }) {
  const { tab } = await searchParams;
  return <AiConnectionPage initialTab={typeof tab === "string" ? tab : undefined} />;
}
