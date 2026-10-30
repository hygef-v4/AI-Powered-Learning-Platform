// U11: Luyện tập Quiz không còn trên menu bên của Student (yêu cầu 2026-10-29); phần Lượt làm và số sinh viên đã làm gắn vào màn Quiz của U10;
// danh sách Quiz gắn học liệu trên Learning Material của U13.

import { learningMaterialSections } from "@/features/materials/slots";
import { quizDetailSections, quizRowActions } from "@/features/quizzes/slots";
import { MaterialQuizList } from "./components/MaterialQuizList";
import { QuizAttemptCount, QuizAttemptsSection } from "./components/TeacherQuizSections";


quizDetailSections.register({ key: "quiz-practice-attempts", order: 10, component: QuizAttemptsSection });
quizRowActions.register({ key: "quiz-practice-count", order: 10, component: QuizAttemptCount });
learningMaterialSections.register({ key: "quiz-practice-material-quizzes", order: 10, component: MaterialQuizList });
