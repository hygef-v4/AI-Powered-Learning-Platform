import type { Metadata } from "next";
import { SubjectMaterialListPage } from "@/features/materials/components/MaterialList";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.materials.page.subjectMaterials") };
}

// U13 Học liệu của môn; `?subjectId=` chọn sẵn môn (sidebar của môn).
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ subjectId?: string }>;
}) {
  const { subjectId } = await searchParams;
  return <SubjectMaterialListPage key={subjectId ?? ""} initialSubjectId={subjectId} />;
}
