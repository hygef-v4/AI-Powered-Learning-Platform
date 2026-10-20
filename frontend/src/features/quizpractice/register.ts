// U11: mục Luyện tập Quiz trên menu bên của Student; phần Lượt làm và số sinh viên đã làm gắn vào màn Quiz của U10.

import { navEntries } from "@/features/identity/slots";
import { quizDetailSections, quizRowActions } from "@/features/quizzes/slots";
import { QuizAttemptCount, QuizAttemptsSection } from "./TeacherQuizSections";

navEntries.register({ key: "quiz-practice", order: 30, title: "Luyện tập Quiz", href: "/quiz-practice", icon: "quiz", roles: ["STUDENT"] });

quizDetailSections.register({ key: "quiz-practice-attempts", order: 10, component: QuizAttemptsSection });
quizRowActions.register({ key: "quiz-practice-count", order: 10, component: QuizAttemptCount });
