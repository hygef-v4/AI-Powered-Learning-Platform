import { SubjectDetailPage } from "@/features/subjects/pages/SubjectDetailPage";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SubjectDetailPage subjectId={id} />;
}
