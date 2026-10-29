import { MyGroupPage } from "@/features/groups";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <MyGroupPage classId={id} />;
}
