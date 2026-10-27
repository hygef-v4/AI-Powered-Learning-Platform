import { redirect } from "next/navigation";

/** Đường dẫn cũ của Lịch sử sử dụng AI: nay là tab Usage trong trang Kết nối AI. */
export default function Page() {
  redirect("/ai/connection?tab=usage");
}
