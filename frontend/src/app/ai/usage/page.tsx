import type { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Lịch sử sử dụng AI" };

/** Đường dẫn cũ của Lịch sử sử dụng AI: nay là tab Usage trong trang Kết nối AI. */
export default function Page() {
  redirect("/ai/connection?tab=usage");
}
