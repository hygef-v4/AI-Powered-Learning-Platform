import type { Metadata } from "next";
import { Suspense } from "react";
import { SubmissionHistoryPage } from "@/features/assignmentwork/pages/SubmissionHistoryPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.myassignments.assignmentId.history.page.lichSuNopBai") };
}

// U08 #41 Submission History (classId trong query).
export default async function Page({ params }: { params: Promise<{ assignmentId: string }> }) {
  const { assignmentId } = await params;
  return <Suspense><SubmissionHistoryPage assignmentId={assignmentId} /></Suspense>;
}
