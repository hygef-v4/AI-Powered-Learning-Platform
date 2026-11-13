import type { Metadata } from "next";
import { QuestionDetailPage } from "@/features/quizzes/pages/QuestionDetailPage";

export const metadata: Metadata = { title: "Câu hỏi" };

// U10 #20 Question Detail; id "new" là thêm câu vào môn ?subjectId=.
export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ subjectId?: string }>;
}) {
  const [{ id }, { subjectId }] = await Promise.all([params, searchParams]);
  return <QuestionDetailPage key={id} id={id} subjectId={subjectId ?? null} />;
}
