import type { Metadata } from "next";
import { QuizResultPage } from "@/features/quizpractice/pages/QuizResultPage";

export const metadata: Metadata = { title: "Kết quả quiz" };

// U11 #45 Quiz Result.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizResultPage key={attemptId} attemptId={attemptId} />;
}
