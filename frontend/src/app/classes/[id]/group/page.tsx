import type { Metadata } from "next";
import { MyGroupPage } from "@/features/groups";

export const metadata: Metadata = { title: "Nhóm" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <MyGroupPage classId={id} />;
}
