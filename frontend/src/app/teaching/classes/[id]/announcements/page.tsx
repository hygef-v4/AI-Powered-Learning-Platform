import type { Metadata } from "next";
import { ClassAnnouncementsPage } from "@/features/announcements/pages/ClassAnnouncementsPage";

export const metadata: Metadata = { title: "Thông báo lớp" };

// U14: Thông báo lớp của một lớp, nằm trong nhánh trang lớp để chuyển qua lại giữa các mục của lớp không dựng lại khung trang.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ClassAnnouncementsPage classId={id} />;
}
