import type { Metadata } from "next";
import { AttemptDiagramPage } from "@/features/assignmentwork/pages/AttemptDiagramPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.attempts.attemptId.diagrams.blockId.page.soDoBaiLam") };
}

// U08: trang xem sơ đồ mở từ link trong DOCX của bài làm.
export default async function Page({ params }: { params: Promise<{ attemptId: string; blockId: string }> }) {
  const { attemptId, blockId } = await params;
  return <AttemptDiagramPage attemptId={attemptId} blockId={blockId} />;
}
