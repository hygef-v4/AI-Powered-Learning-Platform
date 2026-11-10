import type { Metadata } from "next";
import { LearningMaterialPage } from "@/features/materials/pages/LearningMaterialPage";

export const metadata: Metadata = { title: "Học liệu" };
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
