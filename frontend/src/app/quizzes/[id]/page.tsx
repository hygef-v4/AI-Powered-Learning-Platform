import { QuizDetailPage } from "@/features/quizzes/QuizDetailPage";

// U10 #22 Quiz Detail; ?classId= khi giảng viên mở quiz của môn từ lớp mình dạy (chỉ xem).
export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ classId?: string }>;
}) {
  const [{ id }, { classId }] = await Promise.all([params, searchParams]);
  return <QuizDetailPage key={id} id={id} classId={classId ?? null} />;
}
