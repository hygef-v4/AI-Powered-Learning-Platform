import type { Metadata } from "next";
import { AssignmentDiagramPage } from "@/features/assignments/pages/AssignmentDiagramPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.assignments.id.diagrams.blockId.page.soDoBaiTap") };
}

// U07: trang xem sơ đồ mở từ link trong DOCX của đề.
export default async function Page({ params }: { params: Promise<{ id: string; blockId: string }> }) {
  const { id, blockId } = await params;
  return <AssignmentDiagramPage id={id} blockId={blockId} />;
}
