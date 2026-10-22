// U11: mục Luyện tập Quiz trên menu bên của Student; phần Lượt làm và số sinh viên đã làm gắn vào màn Quiz của U10;
// danh sách Quiz gắn học liệu trên Learning Material của U13.

import { navEntries } from "@/features/identity/slots";
import { learningMaterialSections } from "@/features/materials/slots";
import { quizDetailSections, quizRowActions } from "@/features/quizzes/slots";
import { MaterialQuizList } from "./MaterialQuizList";
import { QuizAttemptCount, QuizAttemptsSection } from "./TeacherQuizSections";

navEntries.register({ key: "quiz-practice", order: 30, title: "Luyện tập Quiz", href: "/quiz-practice", icon: "quiz", roles: ["STUDENT"] });

quizDetailSections.register({ key: "quiz-practice-attempts", order: 10, component: QuizAttemptsSection });
quizRowActions.register({ key: "quiz-practice-count", order: 10, component: QuizAttemptCount });
learningMaterialSections.register({ key: "quiz-practice-material-quizzes", order: 10, component: MaterialQuizList });
