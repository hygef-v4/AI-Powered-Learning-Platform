import type { Metadata } from "next";
import { GroupWorkspacePage } from "@/features/groupdocs";

export const metadata: Metadata = { title: "Tài liệu nhóm" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <GroupWorkspacePage documentId={id} />;
}
