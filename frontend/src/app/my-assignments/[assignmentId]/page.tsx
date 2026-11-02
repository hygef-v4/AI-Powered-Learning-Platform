import { Suspense } from "react";
import { StudentAssignmentDetailPage } from "@/features/assignmentwork/StudentAssignmentDetailPage";

// U08 #39 Assignment Detail (classId trong query).
export default async function Page({ params }: { params: Promise<{ assignmentId: string }> }) {
  const { assignmentId } = await params;
  return <Suspense><StudentAssignmentDetailPage assignmentId={assignmentId} /></Suspense>;
}
