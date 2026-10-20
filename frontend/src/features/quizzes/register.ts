// U10: mục Ngân hàng câu hỏi, Quiz trên menu bên của Subject Manager; phần Quiz trong tab Đánh giá và số liệu
// môn trên Manager Dashboard, My Subject (slot của U05).

import { subjectSummarySections, teacherClassTabs } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassQuizzesTab } from "./ClassQuizzesSection";
import { QuizBankSummary } from "./QuizBankSummary";

navEntries.register({ key: "manager-questions", order: 30, title: "Ngân hàng câu hỏi", href: "/manager/questions", icon: "help_center", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-quizzes", order: 40, title: "Quiz", href: "/manager/quizzes", icon: "quiz", roles: ["SUBJECT_MANAGER"] });

teacherClassTabs.register({ key: "quizzes-class-section", order: 20, component: ClassQuizzesTab });
subjectSummarySections.register({ key: "quizzes-bank-summary", order: 20, component: QuizBankSummary });
