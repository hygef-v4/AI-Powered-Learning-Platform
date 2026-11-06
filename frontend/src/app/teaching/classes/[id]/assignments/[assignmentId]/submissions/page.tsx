import { redirect } from "next/navigation";
import { buildGradebookHref } from "@/features/grading/components/ClassProgressSection";

export default async function Page({
  params,
}: {
  params: Promise<{ id: string; assignmentId: string }>;
}) {
  const { id, assignmentId } = await params;
  redirect(buildGradebookHref(id, { assignment: assignmentId }));
}
