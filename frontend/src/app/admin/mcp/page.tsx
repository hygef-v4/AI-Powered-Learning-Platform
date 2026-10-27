import { redirect } from "next/navigation";

/** Đường dẫn cũ của Agent MCP: nay là tab trong trang Kết nối AI (mục Cài đặt). */
export default function Page() {
  redirect("/admin/settings/sections/ai-connections?tab=mcp");
}
