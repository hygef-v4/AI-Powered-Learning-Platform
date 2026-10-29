import { redirect } from "next/navigation";

/** Trang sửa học liệu cũ: nay sửa và lưu trữ ngay trên trang chi tiết (`/view`). */
export default async function Page({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ classId?: string }> }) {
  const { id } = await params; const { classId } = await searchParams;
  redirect(`/materials/${encodeURIComponent(id)}/view${classId ? `?classId=${encodeURIComponent(classId)}` : ""}`);
}
