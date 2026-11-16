import type { Metadata } from "next";
import { QuizPracticeHistoryPage } from "@/features/quizpractice/pages/QuizPracticeHistoryPage";
import { translateOnServer } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await translateOnServer("app.quizpractice.page.luyenTapQuiz") };
}

// U11 #42 Quiz Practice History.
export default function Page() {
  return <QuizPracticeHistoryPage />;
}
