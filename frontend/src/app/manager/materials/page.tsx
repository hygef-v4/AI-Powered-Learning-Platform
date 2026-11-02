import { SubjectMaterialListPage } from "@/features/materials/components/MaterialList";

// U13 Học liệu của môn; `?subjectId=` chọn sẵn môn (sidebar của môn).
export default async function Page({ searchParams }: { searchParams: Promise<{ subjectId?: string }> }) {
  const { subjectId } = await searchParams;
  return <SubjectMaterialListPage key={subjectId ?? ""} initialSubjectId={subjectId} />;
}
