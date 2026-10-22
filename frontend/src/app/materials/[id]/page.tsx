import { MaterialDetailPage } from "@/features/materials/MaterialDetailPage";
export default async function Page({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ classId?: string }> }) {
  const { id } = await params; const { classId } = await searchParams;
  return <MaterialDetailPage key={`${id}:${classId}`} id={id} classId={classId} />;
}
