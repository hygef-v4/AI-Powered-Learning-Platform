import type { Metadata } from "next";
import { Suspense } from "react";
import { StudentAssignmentsPage } from "@/features/assignmentwork/pages/StudentAssignmentsPage";

export const metadata: Metadata = { title: "Bài tập" };

// U08 #38 Student Assignments.
export default function Page() {
  return <Suspense><StudentAssignmentsPage /></Suspense>;
}
