import { ClassAnnouncementsPage } from "@/features/announcements/ClassAnnouncementsPage";

// U14: Thông báo lớp của một lớp, nằm trong nhánh trang lớp để chuyển qua lại giữa các mục của lớp không dựng lại khung trang.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ClassAnnouncementsPage classId={id} />;
}
