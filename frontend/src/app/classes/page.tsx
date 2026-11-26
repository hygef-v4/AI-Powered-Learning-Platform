import type { Metadata } from "next";
import {ClassesRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.classes.page.classes") };
}
// Mở danh sách lớp theo quyền xem do ứng dụng xác định.
export default function Page(){return <ClassesRoute/>;}
