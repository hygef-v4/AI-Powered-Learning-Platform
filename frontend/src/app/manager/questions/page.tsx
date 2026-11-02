import { Suspense } from "react";
import { QuestionListPage } from "@/features/quizzes/pages/QuestionListPage";

// U10 #19 Question List. Trang đọc ?subjectId= bằng useSearchParams nên cần Suspense để vẫn prerender được.
export default function Page() {
  return (
    <Suspense>
      <QuestionListPage />
    </Suspense>
  );
}
