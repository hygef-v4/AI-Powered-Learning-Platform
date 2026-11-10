import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { buildGradebookHref } from "@/features/grading/components/ClassProgressSection";

export const metadata: Metadata = { title: "Bài nộp" };

export default async function Page({
  params,
}: {
  params: Promise<{ id: string; assignmentId: string }>;
}) {
  const { id, assignmentId } = await params;
  redirect(buildGradebookHref(id, { assignment: assignmentId }));
}
