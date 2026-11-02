// U10: mục Ngân hàng câu hỏi trên sidebar của môn (Subject Manager); trang Quiz không còn trên sidebar (yêu cầu 2026-10-29). Tab Bài tập của lớp chỉ có bài tập (yêu cầu 2026-10-29),
// nên phần Quiz của lớp (ClassQuizzesTab) tạm không gắn vào trang lớp.

import { subjectNavEntries } from "@/features/identity/slots";

subjectNavEntries.register({ key: "manager-questions", order: 50, title: "Ngân hàng câu hỏi", icon: "help_center", path: "/manager/questions" });

