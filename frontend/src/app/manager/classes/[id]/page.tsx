import type { Metadata } from "next";
import {ClassDetailRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.classes.id.page.subjectClasses") };
}
// Đưa ID từ URL vào trang lớp sử dụng phạm vi quản lý.
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <ClassDetailRoute id={id} scope="manager"/>;}
