import type { Metadata } from "next";
import { AssignmentDiagramPage } from "@/features/assignments/pages/AssignmentDiagramPage";

export const metadata: Metadata = { title: "Sơ đồ bài tập" };

// U07: trang xem sơ đồ mở từ link trong DOCX của đề.
export default async function Page({ params }: { params: Promise<{ id: string; blockId: string }> }) {
  const { id, blockId } = await params;
  return <AssignmentDiagramPage id={id} blockId={blockId} />;
}
