import type { Metadata } from "next";
import {ClassesRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.teaching.classes.page.assignedClasses") };
}
// Yêu cầu danh sách lớp được phân công cho giảng viên.
export default function Page(){return <ClassesRoute teaching/>;}
