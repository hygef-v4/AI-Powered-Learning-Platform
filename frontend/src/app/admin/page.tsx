import type { Metadata } from "next";
import {AdminDashboardPage} from "@/features/admindashboard/pages/AdminDashboardPage";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.page.overview") };
}

// Mở trang tổng quan trong layout dành cho quản trị viên.
export default function Page() {
  return <AdminDashboardPage/>;
}
