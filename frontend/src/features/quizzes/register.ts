// U10: mục Câu hỏi, Quiz trên sidebar của môn (Subject Manager); phần Quiz trong tab Đánh giá và số liệu
// môn trên Manager Dashboard, My Subject (slot của U05).

import { subjectSummarySections, teacherClassTabs } from "@/features/academics/slots";
import { subjectNavEntries } from "@/features/identity/slots";
import { ClassQuizzesTab } from "./ClassQuizzesSection";
import { QuizBankSummary } from "./QuizBankSummary";

subjectNavEntries.register({ key: "manager-questions", order: 20, title: "Câu hỏi", icon: "help_center", path: "/manager/questions" });
subjectNavEntries.register({ key: "manager-quizzes", order: 30, title: "Quiz", icon: "quiz", path: "/manager/quizzes" });

teacherClassTabs.register({ key: "quizzes-class-section", order: 20, component: ClassQuizzesTab });
subjectSummarySections.register({ key: "quizzes-bank-summary", order: 20, component: QuizBankSummary });
