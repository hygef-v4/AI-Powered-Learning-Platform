import { SubmissionDetailPage } from "@/features/grading";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string; assignmentId: string }>;
}) {
  const { id, assignmentId } = await params;
  return <SubmissionDetailPage classId={id} assignmentId={assignmentId} />;
}
