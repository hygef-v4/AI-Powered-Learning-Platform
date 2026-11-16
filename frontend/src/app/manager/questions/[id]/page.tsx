import type { Metadata } from "next";
import { QuestionDetailPage } from "@/features/quizzes/pages/QuestionDetailPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.manager.questions.id.page.cauHoi") };
}

// U10 #20 Question Detail; id "new" là thêm câu vào môn ?subjectId=.
export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ subjectId?: string }>;
}) {
  const [{ id }, { subjectId }] = await Promise.all([params, searchParams]);
  return <QuestionDetailPage key={id} id={id} subjectId={subjectId ?? null} />;
}
