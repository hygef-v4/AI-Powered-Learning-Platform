import type { Metadata } from "next";
import { Suspense } from "react";
import { SubjectQuizListPage } from "@/features/quizzes/pages/SubjectQuizListPage";

export const metadata: Metadata = { title: "Quiz" };

// U10 #21 Quiz List. Trang đọc ?subjectId= bằng useSearchParams nên cần Suspense để vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <SubjectQuizListPage />
    </Suspense>
  );
}
