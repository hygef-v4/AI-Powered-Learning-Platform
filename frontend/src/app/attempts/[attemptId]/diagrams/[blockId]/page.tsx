import { AttemptDiagramPage } from "@/features/assignmentwork/pages/AttemptDiagramPage";

// U08: trang xem sơ đồ mở từ link trong DOCX của bài làm.
export default async function Page({ params }: { params: Promise<{ attemptId: string; blockId: string }> }) {
  const { attemptId, blockId } = await params;
  return <AttemptDiagramPage attemptId={attemptId} blockId={blockId} />;
}
