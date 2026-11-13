import type { Metadata } from "next";
import { Suspense } from "react";
import { SubjectAssignmentListPage } from "@/features/assignments/pages/SubjectAssignmentListPage";

export const metadata: Metadata = { title: "Bài tập" };

// U07 #25 Assignment List của Chủ nhiệm môn.
export default function Page() {
  return <Suspense><SubjectAssignmentListPage /></Suspense>;
}
