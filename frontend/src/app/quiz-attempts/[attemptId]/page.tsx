import { QuizTakingPage } from "@/features/quizpractice/pages/QuizTakingPage";

// U11 #44 Quiz Taking.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <QuizTakingPage key={attemptId} attemptId={attemptId} />;
}
