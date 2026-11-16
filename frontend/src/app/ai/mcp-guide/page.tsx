import type { Metadata } from "next";
import { McpGuidePage } from "@/features/ai";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.ai.mcpguide.page.huongDanKetNoiMcp") };
}

export default function Page() {
  return <McpGuidePage />;
}
