import type { Metadata } from "next";
import { McpGuidePage } from "@/features/ai";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.ai.mcpguide.page.huongDanKetNoiMcp") };
}

export default function Page() {
  return <McpGuidePage />;
}
