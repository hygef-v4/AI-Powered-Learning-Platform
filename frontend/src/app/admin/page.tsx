import type { Metadata } from "next";
import {AdminDashboardPage} from "@/features/admindashboard/pages/AdminDashboardPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.page.tongQuan") };
}

export default function Page() {
  return <AdminDashboardPage/>;
}
