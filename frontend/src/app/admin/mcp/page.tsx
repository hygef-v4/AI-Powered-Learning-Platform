import { redirect } from "next/navigation";

/** Tương thích bookmark cũ; Admin không còn chức năng kết nối MCP. */
export default function Page() {
  redirect("/admin/settings/sections/ai-connections");
}
