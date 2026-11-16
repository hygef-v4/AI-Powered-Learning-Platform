import type { Metadata } from "next";
import { QuizPracticeDetailPage } from "@/features/quizpractice/pages/QuizPracticeDetailPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.quizpractice.quizId.page.luyenTapQuiz") };
}

// U11 #43 Quiz Practice Detail; ?classId= là lớp người học làm quiz, thiếu thì trang tự tìm lớp (link thông báo của U14).
export default async function Page({ params, searchParams }: {
  params: Promise<{ quizId: string }>;
  searchParams: Promise<{ classId?: string }>;
}) {
  const [{ quizId }, { classId }] = await Promise.all([params, searchParams]);
  return <QuizPracticeDetailPage key={`${quizId}/${classId}`} quizId={quizId} classId={classId || null} />;
}
