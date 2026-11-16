import type { Metadata } from "next";
import { Suspense } from "react";
import { StudentAssignmentsPage } from "@/features/assignmentwork/pages/StudentAssignmentsPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.myassignments.page.baiTap") };
}

// U08 #38 Student Assignments.
export default function Page() {
  return <Suspense><StudentAssignmentsPage /></Suspense>;
}
