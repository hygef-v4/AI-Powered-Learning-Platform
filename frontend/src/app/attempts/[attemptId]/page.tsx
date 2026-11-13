import type { Metadata } from "next";
import { AssignmentWorkspacePage } from "@/features/assignmentwork/pages/AssignmentWorkspacePage";

export const metadata: Metadata = { title: "Làm bài tập" };

// U08 #40 Assignment Workspace.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <AssignmentWorkspacePage attemptId={attemptId} />;
}
