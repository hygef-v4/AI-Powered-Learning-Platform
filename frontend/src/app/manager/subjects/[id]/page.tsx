import type { Metadata } from "next";
import { ManagerRoute } from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.subjects.id.page.thongTinMon") };
}
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ManagerRoute view="subjects" id={id} />;
}
