import { Suspense } from "react";
import { SubmissionHistoryPage } from "@/features/assignmentwork/pages/SubmissionHistoryPage";

// U08 #41 Submission History (classId trong query).
export default async function Page({ params }: { params: Promise<{ assignmentId: string }> }) {
  const { assignmentId } = await params;
  return <Suspense><SubmissionHistoryPage assignmentId={assignmentId} /></Suspense>;
}
