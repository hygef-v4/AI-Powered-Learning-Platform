import type { Metadata } from "next";
import { GroupSectionEditorPage } from "@/features/groupdocs";

export const metadata: Metadata = { title: "Soạn bài nhóm" };

export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ section?: string | string[] }>;
}) {
  const { id } = await params;
  const { section } = await searchParams;
  return <GroupSectionEditorPage documentId={id} focusPart={typeof section === "string" ? section : null} />;
}
