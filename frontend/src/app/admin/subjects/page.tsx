import type { Metadata } from "next";
import { SubjectListPage } from "@/features/subjects/pages/SubjectListPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.admin.subjects.page.monHoc") };
}

export default function Page() {
  return <SubjectListPage />;
}
