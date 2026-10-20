import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";
import { GradingWorkspacePage } from "@/features/grading";
export default async function Page({ params }: { params: Promise<{ evaluationId: string }> }) {
  const { evaluationId } = await params;
  return (
    <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>
      <GradingWorkspacePage evaluationId={evaluationId} />
    </AuthenticatedLayout>
  );
}
