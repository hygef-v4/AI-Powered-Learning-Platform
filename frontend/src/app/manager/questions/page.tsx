import type { Metadata } from "next";
import { Suspense } from "react";
import { QuestionListPage } from "@/features/quizzes/pages/QuestionListPage";
import { serverMsg } from "@/i18n/server";

export async function generateMetadata(): Promise<Metadata> {
  return { title: await serverMsg("app.manager.questions.page.cauHoi") };
}

// U10 #19 Question List. Trang đọc ?subjectId= bằng useSearchParams nên cần Suspense để vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <QuestionListPage />
    </Suspense>
  );
}
