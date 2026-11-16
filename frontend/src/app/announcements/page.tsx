import type { Metadata } from "next";
import { ClassAnnouncementsPage } from "@/features/announcements/pages/ClassAnnouncementsPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.announcements.page.tatCaThongBao") };
}

// U14: Thông báo lớp ở menu chính, mọi lớp; link cũ `?classId=` mở sẵn ô lọc ở lớp đó.
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ classId?: string }>;
}) {
  const { classId } = await searchParams;
  return <ClassAnnouncementsPage initialClassId={classId ?? ""} />;
}
