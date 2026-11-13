import type { Metadata } from "next";
import { Suspense } from "react";
import { StudentAssignmentDetailPage } from "@/features/assignmentwork/pages/StudentAssignmentDetailPage";

export const metadata: Metadata = { title: "Bài tập" };

// U08 #39 Assignment Detail (classId trong query).
export default async function Page({ params }: { params: Promise<{ assignmentId: string }> }) {
  const { assignmentId } = await params;
  return <Suspense><StudentAssignmentDetailPage assignmentId={assignmentId} /></Suspense>;
}
