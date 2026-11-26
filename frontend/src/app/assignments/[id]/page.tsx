import type { Metadata } from "next";
import { Suspense } from "react";
import { AssignmentFormPage } from "@/features/assignments/pages/AssignmentFormPage";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.assignments.id.page.assignment") };
}

// U07 #26 Assignment Form.
// Mở màn soạn theo ID trên URL, nơi link duyệt MCP chọn đề xuất cần xem.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <Suspense><AssignmentFormPage id={id} /></Suspense>;
}
