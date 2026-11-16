import type { Metadata } from "next";
import {AdminDashboardPage} from "@/features/admindashboard/pages/AdminDashboardPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.page.tongQuan") };
}

export default function Page() {
  return <AdminDashboardPage/>;
}
