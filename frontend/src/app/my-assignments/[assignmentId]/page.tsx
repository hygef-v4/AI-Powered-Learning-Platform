import type { Metadata } from "next";
import { Suspense } from "react";
import { StudentAssignmentDetailPage } from "@/features/assignmentwork/pages/StudentAssignmentDetailPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.myassignments.assignmentId.page.baiTap") };
}

// U08 #39 Assignment Detail (classId trong query).
export default async function Page({ params }: { params: Promise<{ assignmentId: string }> }) {
  const { assignmentId } = await params;
  return <Suspense><StudentAssignmentDetailPage assignmentId={assignmentId} /></Suspense>;
}
