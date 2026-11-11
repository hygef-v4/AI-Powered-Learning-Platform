import type { Metadata } from "next";
import { GradingWorkspacePage } from "@/features/grading";

export const metadata: Metadata = { title: "Chấm bài" };
export default async function Page({ params, searchParams }: { params: Promise<{ evaluationId: string }>; searchParams: Promise<{ proposalId?: string | string[]; status?: string | string[]; late?: string | string[] }> }) {
  const { evaluationId } = await params;
  const { proposalId, status, late } = await searchParams;
  return <GradingWorkspacePage evaluationId={evaluationId} proposalId={typeof proposalId === "string" ? proposalId : undefined}
    status={typeof status === "string" ? status : undefined} late={typeof late === "string" ? late : undefined} />;
}
