import { Suspense } from "react";
import { AssignmentFormPage } from "@/features/assignments/AssignmentFormPage";

// U07 #26 Assignment Form.
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <Suspense><AssignmentFormPage id={id} /></Suspense>;
}
