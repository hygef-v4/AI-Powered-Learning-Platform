import { Suspense } from "react";
import { SubjectAssignmentListPage } from "@/features/assignments/pages/SubjectAssignmentListPage";

// U07 #25 Assignment List của Chủ nhiệm môn.
export default function Page() {
  return <Suspense><SubjectAssignmentListPage /></Suspense>;
}
