import type { Metadata } from "next";
import { GroupWorkspacePage } from "@/features/groupdocs";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.groupdocs.id.page.taiLieuNhom") };
}

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <GroupWorkspacePage documentId={id} />;
}
