import type { Metadata } from "next";
import { QuizResultPage } from "@/features/quizpractice/pages/QuizResultPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.quizattempts.attemptId.result.page.ketQuaQuiz") };
}

// U11 #45 Quiz Result.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizResultPage key={attemptId} attemptId={attemptId} />;
}
