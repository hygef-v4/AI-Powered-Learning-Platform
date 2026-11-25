import type { Metadata } from "next";
import { ManagerRoute } from "@/features/academics/pages/ClassRoutes";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.subjects.id.page.subjectInformation") };
}
// Mở thông tin môn cụ thể trong phạm vi quản lý qua ID trên URL.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ManagerRoute view="subjects" id={id} />;
}
