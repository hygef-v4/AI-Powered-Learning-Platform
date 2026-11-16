import type { Metadata } from "next";
import { Suspense } from "react";
import { AssignmentFormPage } from "@/features/assignments/pages/AssignmentFormPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.assignments.id.page.baiTap") };
}

// U07 #26 Assignment Form.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <Suspense><AssignmentFormPage id={id} /></Suspense>;
}
