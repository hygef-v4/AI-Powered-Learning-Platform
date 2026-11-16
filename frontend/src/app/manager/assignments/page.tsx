import type { Metadata } from "next";
import { Suspense } from "react";
import { SubjectAssignmentListPage } from "@/features/assignments/pages/SubjectAssignmentListPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.manager.assignments.page.baiTap") };
}

// U07 #25 Assignment List của Chủ nhiệm môn.
export default function Page() {
  return <Suspense><SubjectAssignmentListPage /></Suspense>;
}
