import type { Metadata } from "next";
import { GroupSectionEditorPage } from "@/features/groupdocs";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.groupdocs.id.edit.page.soanBaiNhom") };
}

export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ section?: string | string[] }>;
}) {
  const { id } = await params;
  const { section } = await searchParams;
  return <GroupSectionEditorPage documentId={id} focusPart={typeof section === "string" ? section : null} />;
}
