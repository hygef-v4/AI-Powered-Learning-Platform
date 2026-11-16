import type { Metadata } from "next";
import { MyGroupPage } from "@/features/groups";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.classes.id.group.page.group") };
}

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <MyGroupPage classId={id} />;
}
