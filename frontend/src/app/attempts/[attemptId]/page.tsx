import type { Metadata } from "next";
import { AssignmentWorkspacePage } from "@/features/assignmentwork/pages/AssignmentWorkspacePage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.attempts.attemptId.page.lamBaiTap") };
}

// U08 #40 Assignment Workspace.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <AssignmentWorkspacePage attemptId={attemptId} />;
}
