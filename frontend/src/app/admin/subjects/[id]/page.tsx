import type { Metadata } from "next";
import { SubjectDetailPage } from "@/features/subjects/pages/SubjectDetailPage";

export const metadata: Metadata = { title: "Môn học" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SubjectDetailPage subjectId={id} />;
}
