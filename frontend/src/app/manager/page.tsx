import type { Metadata } from "next";
import {ManagerRoute} from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.page.overview") };
}
// Mở tổng quan các môn của người quản lý.
export default function Page(){return <ManagerRoute view="overview"/>;}
