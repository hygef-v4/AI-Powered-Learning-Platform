import type { Metadata } from "next";
import { Suspense } from "react";
import { StudentAssignmentsPage } from "@/features/assignmentwork/pages/StudentAssignmentsPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.myassignments.page.baiTap") };
}

// U08 #38 Student Assignments.
export default function Page() {
  return <Suspense><StudentAssignmentsPage /></Suspense>;
}
