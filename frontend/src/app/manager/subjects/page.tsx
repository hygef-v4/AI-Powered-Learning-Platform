import type { Metadata } from "next";
import { ManagerRoute } from "@/features/academics/pages/ClassRoutes";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.manager.subjects.page.thongTinMon") };
}
export default function Page() {
  return <ManagerRoute view="subjects" />;
}
