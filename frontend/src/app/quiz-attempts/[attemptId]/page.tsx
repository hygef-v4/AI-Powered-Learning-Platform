import type { Metadata } from "next";
import { QuizTakingPage } from "@/features/quizpractice/pages/QuizTakingPage";

export const metadata: Metadata = { title: "Làm quiz" };

// U11 #44 Quiz Taking.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizTakingPage key={attemptId} attemptId={attemptId} />;
}
