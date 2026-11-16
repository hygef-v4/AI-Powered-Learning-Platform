import type { Metadata } from "next";
import { QuizTakingPage } from "@/features/quizpractice/pages/QuizTakingPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.quizattempts.attemptId.page.lamQuiz") };
}

// U11 #44 Quiz Taking.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizTakingPage key={attemptId} attemptId={attemptId} />;
}
