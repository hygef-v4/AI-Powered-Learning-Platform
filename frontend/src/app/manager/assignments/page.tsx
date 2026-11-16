import type { Metadata } from "next";
import { Suspense } from "react";
import { SubjectAssignmentListPage } from "@/features/assignments/pages/SubjectAssignmentListPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.manager.assignments.page.assignments") };
}

// U07 #25 Assignment List của Chủ nhiệm môn.
export default function Page() {
  return <Suspense><SubjectAssignmentListPage /></Suspense>;
}
