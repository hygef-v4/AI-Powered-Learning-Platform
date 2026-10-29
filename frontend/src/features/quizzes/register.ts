// U10: mục Câu hỏi, Quiz trên sidebar của môn (Subject Manager). Tab Bài tập của lớp chỉ có bài tập (yêu cầu 2026-10-29),
// nên phần Quiz của lớp (ClassQuizzesTab) tạm không gắn vào trang lớp.

import { subjectNavEntries } from "@/features/identity/slots";

subjectNavEntries.register({ key: "manager-questions", order: 20, title: "Câu hỏi", icon: "help_center", path: "/manager/questions" });
subjectNavEntries.register({ key: "manager-quizzes", order: 30, title: "Quiz", icon: "quiz", path: "/manager/quizzes" });

