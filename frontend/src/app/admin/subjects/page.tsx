import type { Metadata } from "next";
import { SubjectListPage } from "@/features/subjects/pages/SubjectListPage";

export const metadata: Metadata = { title: "Môn học" };

export default function Page() {
  return <SubjectListPage />;
}
