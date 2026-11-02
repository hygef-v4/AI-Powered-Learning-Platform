// U11: Luyện tập Quiz không còn trên menu bên của Student (yêu cầu 2026-10-29); phần Lượt làm và số sinh viên đã làm gắn vào màn Quiz của U10.

import { quizDetailSections, quizRowActions } from "@/features/quizzes/slots";
import { QuizAttemptCount, QuizAttemptsSection } from "./components/TeacherQuizSections";


quizDetailSections.register({ key: "quiz-practice-attempts", order: 10, component: QuizAttemptsSection });
quizRowActions.register({ key: "quiz-practice-count", order: 10, component: QuizAttemptCount });
