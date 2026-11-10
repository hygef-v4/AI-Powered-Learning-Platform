import type { Metadata } from "next";
import {AdminDashboardPage} from "@/features/admindashboard/pages/AdminDashboardPage";

export const metadata: Metadata = { title: "Tổng quan" };

export default function Page() {
  return <AdminDashboardPage/>;
}
