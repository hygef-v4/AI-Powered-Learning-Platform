import { Suspense } from "react";
import { StudentAssignmentsPage } from "@/features/assignmentwork/StudentAssignmentsPage";

// U08 #38 Student Assignments.
export default function Page() {
  return <Suspense><StudentAssignmentsPage /></Suspense>;
}
