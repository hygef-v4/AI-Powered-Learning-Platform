import { GroupWorkspacePage } from "@/features/groupdocs";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <GroupWorkspacePage documentId={id} />;
}
