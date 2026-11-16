import type { Metadata } from "next";
import { LearningMaterialPage } from "@/features/materials/pages/LearningMaterialPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.materials.id.view.page.hocLieu") };
}
export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ classId?: string }>;
}) {
  const { id } = await params;
  const { classId } = await searchParams;
  return <LearningMaterialPage key={`${id}:${classId}`} id={id} classId={classId} />;
}
