import type { Metadata } from "next";
import { SubjectDetailPage } from "@/features/subjects/pages/SubjectDetailPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.admin.subjects.id.page.subjects") };
}

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SubjectDetailPage subjectId={id} />;
}
