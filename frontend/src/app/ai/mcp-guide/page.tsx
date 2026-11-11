import type { Metadata } from "next";
import { McpGuidePage } from "@/features/ai";

export const metadata: Metadata = { title: "Hướng dẫn kết nối MCP" };

export default function Page() {
  return <McpGuidePage />;
}
