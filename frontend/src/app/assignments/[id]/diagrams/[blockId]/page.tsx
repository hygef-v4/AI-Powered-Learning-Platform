import { AssignmentDiagramPage } from "@/features/assignments/pages/AssignmentDiagramPage";

// U07: trang xem sơ đồ mở từ link trong DOCX của đề.
export default async function Page({ params }: { params: Promise<{ id: string; blockId: string }> }) {
  const { id, blockId } = await params;
  return <AssignmentDiagramPage id={id} blockId={blockId} />;
}
