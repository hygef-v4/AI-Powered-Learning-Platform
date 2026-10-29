import { GradingWorkspacePage } from "@/features/grading";
export default async function Page({ params }: { params: Promise<{ evaluationId: string }> }) {
  const { evaluationId } = await params;
  return <GradingWorkspacePage evaluationId={evaluationId} />;
}
