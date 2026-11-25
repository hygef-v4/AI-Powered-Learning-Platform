import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.classes.page.subjectClasses") };
}
// Chọn môn ban đầu theo subjectId trên URL khi mở danh sách lớp quản lý.
export default async function Page({searchParams}:{searchParams:Promise<{subjectId?:string}>}){const {subjectId}=await searchParams;return <ManagerRoute view="classes" initialSubjectId={subjectId}/>;}
