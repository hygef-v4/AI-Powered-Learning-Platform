import type { Metadata } from "next";
import { GradingWorkspacePage } from "@/features/grading";
import { translateOnServer } from "@/i18n/server";

// Đặt tiêu đề tab theo ngôn ngữ hiện tại ngay trên server.
export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.teaching.evaluations.evaluationId.page.gradeSubmission") };
}
// Đọc ID bản chấm, đề xuất và bộ lọc hợp lệ từ URL trước khi mở workspace.
export default async function Page({ params, searchParams }: { params: Promise<{ evaluationId: string }>; searchParams: Promise<{ proposalId?: string | string[]; status?: string | string[]; late?: string | string[] }> }) {
  const { evaluationId } = await params;
  const { proposalId, status, late } = await searchParams;
  return <GradingWorkspacePage evaluationId={evaluationId} proposalId={typeof proposalId === "string" ? proposalId : undefined}
    status={typeof status === "string" ? status : undefined} late={typeof late === "string" ? late : undefined} />;
}
