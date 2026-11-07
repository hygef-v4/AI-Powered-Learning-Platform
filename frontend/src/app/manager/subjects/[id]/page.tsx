import { ManagerRoute } from "@/features/academics/pages/ClassRoutes";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ManagerRoute view="subjects" id={id} />;
}
