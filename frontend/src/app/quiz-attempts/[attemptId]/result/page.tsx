import { QuizResultPage } from "@/features/quizpractice/QuizResultPage";

// U11 #45 Quiz Result.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizResultPage key={attemptId} attemptId={attemptId} />;
}
