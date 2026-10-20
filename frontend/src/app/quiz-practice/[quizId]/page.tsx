import { QuizPracticeDetailPage } from "@/features/quizpractice/QuizPracticeDetailPage";

// U11 #43 Quiz Practice Detail; ?classId= là lớp người học làm quiz.
export default async function Page({ params, searchParams }: {
  params: Promise<{ quizId: string }>;
  searchParams: Promise<{ classId?: string }>;
}) {
  const [{ quizId }, { classId }] = await Promise.all([params, searchParams]);
  return <QuizPracticeDetailPage key={`${quizId}/${classId}`} quizId={quizId} classId={classId ?? ""} />;
}
