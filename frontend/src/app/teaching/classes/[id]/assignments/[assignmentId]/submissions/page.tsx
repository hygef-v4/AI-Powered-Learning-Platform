import { AuthenticatedLayout } from "@/features/identity/shell/AuthenticatedLayout";
import { SubmissionDetailPage } from "@/features/grading";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string; assignmentId: string }>;
}) {
  const { id, assignmentId } = await params;
  return (
    <AuthenticatedLayout roles={["TEACHER", "SUBJECT_MANAGER"]}>
      <SubmissionDetailPage classId={id} assignmentId={assignmentId} />
    </AuthenticatedLayout>
  );
}
