import type { Metadata } from "next";
import { QuizPracticeHistoryPage } from "@/features/quizpractice/pages/QuizPracticeHistoryPage";

export const metadata: Metadata = { title: "Luyện tập Quiz" };

// U11 #42 Quiz Practice History.
export default function Page() {
  return <QuizPracticeHistoryPage />;
}
