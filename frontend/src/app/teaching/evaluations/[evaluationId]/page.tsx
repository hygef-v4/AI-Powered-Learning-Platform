import { GradingWorkspacePage } from "@/features/grading";
export default async function Page({ params, searchParams }: { params: Promise<{ evaluationId: string }>; searchParams: Promise<{ proposalId?: string | string[] }> }) {
  const { evaluationId } = await params;
  const { proposalId } = await searchParams;
  return <GradingWorkspacePage evaluationId={evaluationId} proposalId={typeof proposalId === "string" ? proposalId : undefined} />;
}
