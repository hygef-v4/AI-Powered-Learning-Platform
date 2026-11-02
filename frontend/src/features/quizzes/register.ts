// U10: mục Ngân hàng câu hỏi trên sidebar của môn (Subject Manager); trang Quiz không còn trên sidebar (yêu cầu 2026-10-29). Tab Bài tập của lớp chỉ có bài tập (yêu cầu 2026-10-29),
// nên phần Quiz của lớp (ClassQuizzesTab) tạm không gắn vào trang lớp. Câu giảng viên đề xuất vào ngân hàng nằm ở tab Học liệu, nơi có Quiz của lớp.

import { createElement } from "react";
import { teacherClassTabs } from "@/features/academics/slots";
import { subjectNavEntries } from "@/features/identity/slots";
import { QuestionProposalsSection } from "./components/QuestionProposalsSection";

teacherClassTabs.register({ key: "quiz-question-proposals", order: 60, component: ({ classId, tab }) => tab === "materials" ? createElement(QuestionProposalsSection, { classId }) : null });
subjectNavEntries.register({ key: "manager-questions", order: 50, title: "Ngân hàng câu hỏi", icon: "help_center", path: "/manager/questions" });

